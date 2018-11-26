package com.zehhow.jikevideodownloader.download;

import android.os.AsyncTask;
import android.util.Log;

import com.zehhow.jikevideodownloader.dao.SQLiteHelper;
import com.zehhow.jikevideodownloader.dao.TaskBean;
import com.zehhow.jikevideodownloader.okHttp.HttpClient;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Vector;

import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<TaskBean, Integer, TaskStatus> {

    private DownloadListener listener;
    private TaskStatus taskStatus = TaskStatus.DOWNLOADING;
    private TaskBean task;          // 任务信息
    private int lastProgress = 0;   // 上一次的下载进度

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 后台下载过程
     * @param taskBeans 任务信息类
     * @return 下载结果
     */
    @Override
    protected TaskStatus doInBackground(TaskBean... taskBeans) {
        task = taskBeans[0];
        if(task.url == null || task.url.isEmpty()) return TaskStatus.FAILED;

        Log.d("JKVD", "------URL After AsyncTask: " + task.url);
        // 判断目录是否存在，不存在则创建
        File file = new File(task.path);
        if(!file.exists()) {
            if(!file.mkdirs()) {
                Log.d("JKVD", " Failed to mkdir | " + task.path);
                return TaskStatus.FAILED;
            }
            Log.d("JKVD", " Dir not exist | " + task.path);
        }

        // 获取视频的m3u8播放列表地址
        String m3u8 = DownloadUtil.getM3u8Url(task.url);
        if(m3u8 == null || m3u8.isEmpty()) return TaskStatus.FAILED;

        // 如果未指定已下载长度、总长度，则从数据库中获取
        if(task.downloadedLength == 0)
            task.downloadedLength = SQLiteHelper.getInstance().getDownloadedLength(task.urlHashCode);
        if(task.totalLength == 0)
            task.totalLength = SQLiteHelper.getInstance().getTotalLength(task.urlHashCode);

        // 下载已完成，则退出下载
        if(task.totalLength > 0 && task.downloadedLength == task.totalLength) {
            Log.d("JKVD", " File has downloaded");
            return TaskStatus.SUCCESS;
        }

        // 获取所有ts分段的下载地址及文件长度大小
        Vector<String> tsUrls = DownloadUtil.getAllTsUrls(m3u8);
        Vector<Long> tsLengths = DownloadUtil.getVedioLength(tsUrls);

        // 若任务在数据库中不存在，则添加任务记录至数据库
        if(task.downloadedLength == -1) {
            task.downloadedLength = 0;
            // 计算所有ts分段的总长度
            task.totalLength = 0;
            for (long l : tsLengths) {
                task.totalLength += l;
            }
            if(task.totalLength == 0) return TaskStatus.FAILED;
            SQLiteHelper.getInstance().addTask(task);
        }

        // 如果文件被删除则重新下载
        file = new File(task.path, task.name);
        if(!file.exists()) {
            task.downloadedLength = 0;
            Log.d("JKVD", " File deleted | " + file.toString());
        }

        Log.d("JKVD", " Start downloading: " + task.name);
        Log.d("JKVD", " DownloadedLength: " + task.downloadedLength + " | " + task.name);
        Log.d("JKVD", " TotalLength: " + task.totalLength + " | " + task.name);

        /* 开始下载各个ts分段 */

        int tsIndex = 0;                // 正在下载的ts分段在Vector里的下标
        int tsSize = tsUrls.size();
        long curDownloadedLength;       // 正在下载的ts分段的已下载长度
        RandomAccessFile savedFile = null;

        // 根据已下载的文件长度定位ts分段位置
        curDownloadedLength = task.downloadedLength;
        while(tsIndex < tsSize && curDownloadedLength >= tsLengths.get(tsIndex)) {
            curDownloadedLength -= tsLengths.get(tsIndex);
            tsIndex++;
        }

        try {
            savedFile = new RandomAccessFile(file, "rw");
            savedFile.seek(task.downloadedLength);  // 定位文件指针至已下载处

            // 依次下载各个ts分段
            for( ; tsIndex < tsSize; tsIndex++) {
                curDownloadedLength = downloadTs(savedFile, tsUrls.get(tsIndex), curDownloadedLength);

                // 如果下载状态不正常则退出下载
                if(taskStatus != TaskStatus.DOWNLOADING) return taskStatus;
                if(curDownloadedLength == -1) return TaskStatus.FAILED;

                // 此时当前的ts分段应已下载完毕，进行下一个分段的下载
                if(curDownloadedLength >= tsLengths.get(tsIndex))
                    curDownloadedLength = 0;
                else    // 若未下载完成说明出现错误
                    return TaskStatus.FAILED;
            }

            return TaskStatus.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return TaskStatus.FAILED;
        } finally {
            try {
                if(savedFile != null) savedFile.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载一个ts分段文件
     * @param savedFile 数据保存文件
     * @param tsUrl ts分段地址
     * @param downloadedLength 该ts分段的已下载长度
     * @return ts分段最新的已下载长度
     */
    private long downloadTs(RandomAccessFile savedFile, String tsUrl, long downloadedLength) {
        if(savedFile == null || tsUrl == null || tsUrl.isEmpty()) return -1;

        InputStream is = null;
        try {
            Request request = new Request.Builder()
                    // 跳过已下载部分
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(tsUrl).build();
            Response response = HttpClient.getInstance().newCall(request).execute();
            if(response == null || response.body() == null) return -1;

            is = response.body().byteStream();
            int len;
            byte[] b = new byte[1024];
            // 读取数据并写入文件
            while((len = is.read(b)) != -1) {
                // 如果下载状态不正常则退出下载
                if(taskStatus != TaskStatus.DOWNLOADING) return -1;

                savedFile.write(b, 0, len);
                downloadedLength += len;
                task.downloadedLength += len;
                // 更新进度信息
                int progress = (int)(task.downloadedLength * 100 / task.totalLength);
                publishProgress(progress);
            }

            response.body().close();
            return downloadedLength;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 更新进度
     * @param values values[0]为进度百分比
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress > lastProgress) {
            Log.d("JKVD", " Progress: " + progress + " | " + task.name);
            Log.d("JKVD", " DownloadedLength: " + task.downloadedLength + " | " + task.name);
            listener.onProgress(progress);
            SQLiteHelper.getInstance().updateProgress(task.urlHashCode, task.downloadedLength, progress);
            lastProgress = progress;
        }
    }

    /**
     * 下载结束时调用此函数
     * @param status 任务状态，及下载结果
     */
    @Override
    protected void onPostExecute(TaskStatus status) {
        switch(status) {
            case SUCCESS:
                listener.onSuceess();
                break;
            case FAILED:
                listener.onFailed();
                break;
            case PAUSED:
                listener.onPaused();
                break;
            case CANCELED:
                // 取消任务时删除文件，以及删除任务记录
                File file = new File(task.path, task.name);
                if(file.exists()) file.delete();
                SQLiteHelper.getInstance().deleteTask(task.urlHashCode);
                listener.onCanceled();
                break;
            default:
                break;
        }
    }

    /**
     * 暂停下载
     */
    public void pausedDownload() {
        taskStatus = TaskStatus.PAUSED;
    }

    /**
     * 取消下载
     */
    public void cancelDownload() {
        taskStatus = TaskStatus.CANCELED;
    }
}
