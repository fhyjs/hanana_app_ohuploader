package org.eu.hanana.reimu.app.webui.ohuploader.ffmpeg;

import com.nextbreakpoint.ffmpeg4java.AVCodecContext;
import com.nextbreakpoint.ffmpeg4java.AVCodecParameters;
import com.nextbreakpoint.ffmpeg4java.AVFormatContext;
import com.nextbreakpoint.ffmpeg4java.AVStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eu.hanana.reimu.app.webui.ohuploader.Util;
import org.eu.hanana.reimu.app.webui.ohuploader.util.OSType;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.nextbreakpoint.ffmpeg4java.Libffmpeg.*;
import static com.nextbreakpoint.ffmpeg4java.Libffmpeg_1.*;
import static com.nextbreakpoint.ffmpeg4java.Libffmpeg_2.C_POINTER;
import static java.lang.foreign.MemorySegment.NULL;
import static org.eu.hanana.reimu.app.webui.ohuploader.test.ConvertVideoMain.decodeVideoError;

public class Video implements AutoCloseable{
    private static boolean loaded = false;
    //总比特数 = 目标大小（字节） × 8
    //码率（bps）= 总比特数 / 视频时长（秒） - 音频码率

    private static final Logger log = LogManager.getLogger(Video.class);
    private final Arena arena;
    private MemorySegment pInputFileName = MemorySegment.NULL;
    public MemorySegment ppInputFormatCtx;
    public MemorySegment pInputFormatCtx;
    public MemorySegment pInputCodecParameters= NULL;
    public MemorySegment pInputCodec = NULL;
    public MemorySegment pInputCodecCtx;

    public Video(){
        arena = Arena.ofConfined();
        try {
            if (!loaded) {
                loadLib();
                // 定义回调函数
                av_log_set_level(AV_LOG_INFO());
                avformat_network_init();
                System.out.println("avformat license : "+avformat_license().getString(0));
                loaded=true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ppInputFormatCtx=arena.allocate(C_POINTER);
    }
    public void setInput(String url){
        pInputFileName = arena.allocateFrom(url);
        if (avformat_open_input(ppInputFormatCtx, pInputFileName, NULL, NULL) != 0) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't open file"));
        }
        pInputFormatCtx = ppInputFormatCtx.get(C_POINTER, 0);

        if (avformat_find_stream_info(pInputFormatCtx, NULL) != 0) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't find stream info"));
        }

        av_dump_format(pInputFormatCtx, 0, pInputFileName, 0);
        var inputNbStreams = AVFormatContext.nb_streams(pInputFormatCtx);
        var pInputStreams = AVFormatContext.streams(pInputFormatCtx);

        int videoStreamIndex = -1;
        pInputCodecParameters = NULL;
        pInputCodec = NULL;

        for (int i = 0; i < inputNbStreams; i++) {
            final var pStream = pInputStreams.getAtIndex(C_POINTER, i);

            final var pVideoCodecParameters = AVStream.codecpar(pStream);

            final var codecType = AVCodecParameters.codec_type(pVideoCodecParameters);

            if (codecType == AVMEDIA_TYPE_VIDEO()) {
                videoStreamIndex = i;
                pInputCodecParameters = pVideoCodecParameters;
                pInputCodec = avcodec_find_decoder(AVCodecParameters.codec_id(pVideoCodecParameters));
                break;
            }
        }

        if (videoStreamIndex == -1) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't find video stream"));
        }

        if (pInputCodec.equals(NULL)) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't find decoder"));
        }

        pInputCodecCtx = avcodec_alloc_context3(pInputCodec);
        if (avcodec_parameters_to_context(pInputCodecCtx, pInputCodecParameters) != 0) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't copy codec parameters"));
        }

        if (pInputCodecCtx.equals(NULL)) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't allocate codec context"));
        }

        if (avcodec_open2(pInputCodecCtx, pInputCodec, NULL) < 0) {
            throw new RuntimeException(decodeVideoError(getInputFileName(), "Can't open decoder"));
        }

    }
    public int getFrameWidth(){
        return AVCodecContext.width(pInputCodecCtx);
    }
    public int getFrameHeight(){
        return AVCodecContext.height(pInputCodecCtx);
    }
    public int getPixelFormat(){
        return AVCodecContext.pix_fmt(pInputCodecCtx);
    }
    public long getDuration(TimeUnit timeUnit){
        return timeUnit.convert(Duration.of( AVFormatContext.duration(this.pInputFormatCtx),TimeUnit.MICROSECONDS.toChronoUnit()));
    }
    public String getInputFileName() {
        return pInputFileName.getString(0);
    }
    @SuppressWarnings({"deprecated", "removal"})
    public static void loadLib() throws IOException {
        File file = new File("tmp/libs");
        file.mkdirs();
        if (!System.getProperty("java.library.path").contains(file.getAbsolutePath())){
            System.setProperty("java.library.path",System.getProperty("java.library.path")+";"+file.getAbsoluteFile()+";");
            try {
                Class<?> libraryPathsClass = Class.forName("jdk.internal.loader.NativeLibraries$LibraryPaths");
                Class<?> usClass = Class.forName("sun.misc.Unsafe");
                Field theUnsafe = usClass.getDeclaredField("theUnsafe");
                theUnsafe.trySetAccessible();
                Unsafe unsafe = (Unsafe) theUnsafe.get(null);
                Field declaredField = libraryPathsClass.getDeclaredField("USER_PATHS");
                long l = unsafe.staticFieldOffset(declaredField);
                String[] object = (String[]) unsafe.getObject(unsafe.staticFieldBase(declaredField), l);
                List<String> list = new ArrayList<>(Arrays.stream(object).toList());
                list.add(file.getAbsoluteFile().getAbsolutePath());
                unsafe.getAndSetObject(unsafe.staticFieldBase(declaredField), l,list.toArray());
                //unsafe.getAndSetObject(null,l,)
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("Can not automatic set lib path!");
            }
        }
        var libFile = new File(file,"ffmpeg4java."+ OSType.getOS().linkFileExt);
        libFile.createNewFile();
        var is = Video.class.getClassLoader().getResourceAsStream(libFile.getName());
        Files.write(libFile.toPath(),is.readAllBytes());
        is.close();
    }
    @Override
    public void close() {
        if (!pInputCodecCtx.equals(NULL)) {
            avcodec_close(pInputCodecCtx);
        }
        if (!ppInputFormatCtx.equals(NULL)) {
            avformat_close_input(ppInputFormatCtx);
        }
        arena.close();
    }
}
