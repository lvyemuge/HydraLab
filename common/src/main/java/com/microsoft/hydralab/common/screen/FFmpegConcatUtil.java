// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.hydralab.common.screen;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.microsoft.hydralab.common.util.Const;
import com.microsoft.hydralab.common.util.CommandOutputReceiver;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FFmpegConcatUtil {

    public static File concatVideos(List<File> videos, File outputDir, Logger logger) {
        if (videos.isEmpty()) {
            return null;
        }
        final String fileName = Const.ScreenRecoderConfig.DEFAULT_FILE_NAME;
        if (videos.size() <= 1) {
            File file = videos.get(0);
            Assert.isTrue(file.renameTo(new File(file.getParentFile(), fileName)), "rename fail");
            return file;
        }
        File file = new File(outputDir, "list.txt");
        StringBuilder stringBuilder = new StringBuilder();
        for (File video : videos) {
            stringBuilder.append(String.format("file '%s'\n", video.getAbsolutePath()));
        }
        FileUtil.writeString(stringBuilder.toString(), file, StandardCharsets.UTF_8);
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"ffmpeg", "-f", "concat", "-safe", "0", "-i", "list.txt", "-c", "copy", fileName}, null, outputDir);
            try (InputStream inputStream = process.getInputStream()) {
                logger.info(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            }
            if (Thread.currentThread().isInterrupted()) {
                // The interrupted status of the thread is cleared by this method.
                Assert.isTrue(Thread.interrupted());
            }


            process.waitFor();
            return new File(outputDir, fileName);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return null;
    }

    public static void mergeVideosSideBySide(String leftVideoPath, String rightVideoPath, String mergeDestinationPath, Logger logger) {
        try {
            ProcessBuilder builder = new ProcessBuilder("ffmpeg", "-i", rightVideoPath,
                    "-i", leftVideoPath, "-filter_complex",
                    "[1][0]scale2ref=trunc((oh*mdar)/2)*2:ih[2nd][ref];[ref][2nd]hstack", "-c:v", "libx264",
                    "-crf", "23", "-vsync", "2", "-preset", "veryfast", mergeDestinationPath);
            Process powerShellProcess = builder.inheritIO().start();
            CommandOutputReceiver err = new CommandOutputReceiver(powerShellProcess.getErrorStream(), logger);
            CommandOutputReceiver out = new CommandOutputReceiver(powerShellProcess.getInputStream(), logger);
            err.start();
            out.start();
            powerShellProcess.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
