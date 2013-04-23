package com.ee.tayra.io.reader.nio;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

import com.ee.tayra.io.reader.nio.Chunker.PartialDocumentHandler;


public class Chunk implements Iterable<String> {

  private static final int ONE_KB = 1024;
  private static final int SIZE = 8 * ONE_KB * ONE_KB;
  private int readSize;
  private MappedByteBuffer chunk;
  private final PartialDocumentHandler handler;

  public Chunk(FileChannel channel, long filePointer, long fileLength,
      PartialDocumentHandler handler) throws IOException {
    this.handler = handler;
    readSize = (int) Math.min(SIZE, fileLength - filePointer);
    chunk = channel.map(FileChannel.MapMode.READ_ONLY, filePointer,
        readSize);
  }

  public int getReadSize() {
    return readSize;
  }

  @Override
  public Iterator<String> iterator() {
    try {
      return new DocumentIterator(chunk, handler);
    } catch (CharacterCodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static class DocumentIterator implements Iterator<String> {

    private final CharBuffer charBuffer;
    private final String[] documents;
    private int index = 0;
    private final PartialDocumentHandler handler;

    public DocumentIterator(MappedByteBuffer chunk,
        PartialDocumentHandler handler) throws CharacterCodingException {
      this.handler = handler;
      Charset charset = Charset.defaultCharset();
      CharsetDecoder decoder = charset.newDecoder();
      this.charBuffer = decoder.decode(chunk);
      // TODO: preferably traverse charBuffer
      documents = charBuffer.toString().split("\\n");
      documents[0] = handler.prependPartialDocumentTo(documents[0]);
    }

    @Override
    public boolean hasNext() {
      if (isLastDocumentPartial()) {
        handler.handlePartialDocument(documents[index]);
        return false;
      }
      return documents.length > index;
    }

    private boolean isLastDocumentPartial() {
      if (isLastDocument()) {
        String lastDocument = documents[index];
        return isPartial(lastDocument);
      } else {
        return false;
      }
    }

    boolean isLastDocument() {
      return index == documents.length - 1;
    }

    boolean isPartial(String document) {
      return !(document.contains("{") && document.contains("}}"));
    }

    @Override
    public String next() {
      // TODO: throw new IllegalStateException when index is out of bounds
      return documents[index++].trim();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("remove not supported");
    }
  }
}