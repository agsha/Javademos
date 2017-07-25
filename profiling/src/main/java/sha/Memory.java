package sha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class Memory
{
    private static final Logger log = LogManager.getLogger();
    private static Settings s;

    public static void main( String[] args ) throws Exception {
        Memory obj = new Memory();

        obj.consumeMemory();

    }

    public static class Settings {
        public String dummy = "";
        // required for jackson
        public Settings() {
        }
    }


    /**
     * All teh code from here:
     */
    private void go() throws Exception {
        Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
        singleoneInstanceField.setAccessible(true);
        Unsafe s =  (Unsafe) singleoneInstanceField.get(null);

//        System.out.println("hello");
        List<ByteBuffer> list = new ArrayList<>();
        int count = 0;
        Thread.sleep(5000);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    System.out.println(String.format("live objects:%d gc objects:%d", MyDeflater.numObjects.get(), MyDeflater.finalizeCalled.get()));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
        List<MyGZIPOutputStream> mylist = new ArrayList<>();
        int random = ThreadLocalRandom.current().nextInt(500, 1000);
        System.out.println("next cleaup at "+random);
        int last = 0;
        while(true) {
//            list.add(ByteBuffer.allocateDirect(1024*1024));
            FileOutputStream outputStream     = new FileOutputStream("/home/sharath.g/peace_essay.ZIP");
            MyGZIPOutputStream gzipOutputStream = new MyGZIPOutputStream(outputStream);
            mylist.add(gzipOutputStream);
            if(count%2000==0) {
                System.out.printf("allocated MB:%d countdown to next cleanup:%d\n", count, random - last);

                Thread.sleep(1000);
            }
            if(last==random) {
                for (MyGZIPOutputStream stream : mylist) {
                    stream.close();
                }
                mylist.clear();
                last = 0;
                random = ThreadLocalRandom.current().nextInt(500, 1000);
                System.out.println("next cleanup at "+random);

            }

            count++;
            last++;
        }

    }

    public void consumeMemory() throws NoSuchFieldException, IllegalAccessException {
        Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
        singleoneInstanceField.setAccessible(true);
        Unsafe s =  (Unsafe) singleoneInstanceField.get(null);
        int c = 0;
        while(true) {
            s.allocateMemory(1024);
            c++;
            if(c%10000000 ==0) {
                System.out.printf("allocated %d KB\n", c);
            }
        }
    }
    public void go2() throws Exception {
        while(true) {
            doGo();
        }
    }
    int count = 0;
    private void doGo() {
        if(count % 100_000_000 == 0) {
            System.out.println(count);
        }
        count++;
    }

    static class MyDeflater extends Deflater {
        public static AtomicLong finalizeCalled = new AtomicLong();
        public static AtomicLong numObjects = new AtomicLong();
        @Override
        public void finalize() {
            super.finalize();
            finalizeCalled.incrementAndGet();
        }
        public MyDeflater(int level, boolean nowrap) {
            super(level, nowrap);
            numObjects.incrementAndGet();
        }

    }
    static class MyGZIPOutputStream extends DeflaterOutputStream {
        /**
         * CRC-32 of uncompressed data.
         */
        protected CRC32 crc = new CRC32();

        /*
         * GZIP header magic number.
         */
        private final static int GZIP_MAGIC = 0x8b1f;

        /*
         * Trailer size in bytes.
         *
         */
        private final static int TRAILER_SIZE = 8;

        /**
         * Creates a new output stream with the specified buffer size.
         *
         * <p>The new output stream instance is created as if by invoking
         * the 3-argument constructor GZIPOutputStream(out, size, false).
         *
         * @param out the output stream
         * @param size the output buffer size
         * @exception IOException If an I/O error has occurred.
         * @exception IllegalArgumentException if {@code size <= 0}
         */
        public MyGZIPOutputStream(OutputStream out, int size) throws IOException {
            this(out, size, false);
        }

        /**
         * Creates a new output stream with the specified buffer size and
         * flush mode.
         *
         * @param out the output stream
         * @param size the output buffer size
         * @param syncFlush
         *        if {@code true} invocation of the inherited
         *        {@link DeflaterOutputStream#flush() flush()} method of
         *        this instance flushes the compressor with flush mode
         *        {@link Deflater#SYNC_FLUSH} before flushing the output
         *        stream, otherwise only flushes the output stream
         * @exception IOException If an I/O error has occurred.
         * @exception IllegalArgumentException if {@code size <= 0}
         *
         * @since 1.7
         */
        public MyGZIPOutputStream(OutputStream out, int size, boolean syncFlush)
                throws IOException
        {
            super(out, new MyDeflater(Deflater.DEFAULT_COMPRESSION, true),
                    size,
                    syncFlush);
            try {
                Field usesDefaultDeflater = DeflaterOutputStream.class.getDeclaredField("usesDefaultDeflater");
                usesDefaultDeflater.setAccessible(true);
                usesDefaultDeflater.set(this, true);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            writeHeader();
            crc.reset();
        }


        /**
         * Creates a new output stream with a default buffer size.
         *
         * <p>The new output stream instance is created as if by invoking
         * the 2-argument constructor GZIPOutputStream(out, false).
         *
         * @param out the output stream
         * @exception IOException If an I/O error has occurred.
         */
        public MyGZIPOutputStream(OutputStream out) throws IOException {
            this(out, 512, false);
        }

        /**
         * Creates a new output stream with a default buffer size and
         * the specified flush mode.
         *
         * @param out the output stream
         * @param syncFlush
         *        if {@code true} invocation of the inherited
         *        {@link DeflaterOutputStream#flush() flush()} method of
         *        this instance flushes the compressor with flush mode
         *        {@link Deflater#SYNC_FLUSH} before flushing the output
         *        stream, otherwise only flushes the output stream
         *
         * @exception IOException If an I/O error has occurred.
         *
         * @since 1.7
         */
        public MyGZIPOutputStream(OutputStream out, boolean syncFlush)
                throws IOException
        {
            this(out, 512, syncFlush);
        }

        /**
         * Writes array of bytes to the compressed output stream. This method
         * will block until all the bytes are written.
         * @param buf the data to be written
         * @param off the start offset of the data
         * @param len the length of the data
         * @exception IOException If an I/O error has occurred.
         */
        public synchronized void write(byte[] buf, int off, int len)
                throws IOException
        {
            super.write(buf, off, len);
            crc.update(buf, off, len);
        }

        /**
         * Finishes writing compressed data to the output stream without closing
         * the underlying stream. Use this method when applying multiple filters
         * in succession to the same output stream.
         * @exception IOException if an I/O error has occurred
         */
        public void finish() throws IOException {
            if (!def.finished()) {
                def.finish();
                while (!def.finished()) {
                    int len = def.deflate(buf, 0, buf.length);
                    if (def.finished() && len <= buf.length - TRAILER_SIZE) {
                        // last deflater buffer. Fit trailer at the end
                        writeTrailer(buf, len);
                        len = len + TRAILER_SIZE;
                        out.write(buf, 0, len);
                        return;
                    }
                    if (len > 0)
                        out.write(buf, 0, len);
                }
                // if we can't fit the trailer at the end of the last
                // deflater buffer, we write it separately
                byte[] trailer = new byte[TRAILER_SIZE];
                writeTrailer(trailer, 0);
                out.write(trailer);
            }
        }

        /*
         * Writes GZIP member header.
         */
        private void writeHeader() throws IOException {
            out.write(new byte[] {
                    (byte) GZIP_MAGIC,        // Magic number (short)
                    (byte)(GZIP_MAGIC >> 8),  // Magic number (short)
                    Deflater.DEFLATED,        // Compression method (CM)
                    0,                        // Flags (FLG)
                    0,                        // Modification time MTIME (int)
                    0,                        // Modification time MTIME (int)
                    0,                        // Modification time MTIME (int)
                    0,                        // Modification time MTIME (int)
                    0,                        // Extra flags (XFLG)
                    0                         // Operating system (OS)
            });
        }

        /*
         * Writes GZIP member trailer to a byte array, starting at a given
         * offset.
         */
        private void writeTrailer(byte[] buf, int offset) throws IOException {
            writeInt((int)crc.getValue(), buf, offset); // CRC-32 of uncompr. data
            writeInt(def.getTotalIn(), buf, offset + 4); // Number of uncompr. bytes
        }

        /*
         * Writes integer in Intel byte order to a byte array, starting at a
         * given offset.
         */
        private void writeInt(int i, byte[] buf, int offset) throws IOException {
            writeShort(i & 0xffff, buf, offset);
            writeShort((i >> 16) & 0xffff, buf, offset + 2);
        }

        /*
         * Writes short integer in Intel byte order to a byte array, starting
         * at a given offset
         */
        private void writeShort(int s, byte[] buf, int offset) throws IOException {
            buf[offset] = (byte)(s & 0xff);
            buf[offset + 1] = (byte)((s >> 8) & 0xff);
        }

    }
}
