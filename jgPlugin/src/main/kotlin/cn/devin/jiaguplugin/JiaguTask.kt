package cn.devin.jiaguplugin

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.and
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by devin on 2021/12/31.
 */

open class JiaguTask @Inject constructor(
    val apkFile: File,
    val jiaguParams: JiaguParams,
) : DefaultTask() {
    init {
        group = "jiagu"
    }

    @TaskAction
    fun jiagu() {
        println(jiaguParams.versionName)
        println(jiaguParams.versionCode)
        println(jiaguParams.sid)
        println("文件名："+apkFile.name)
        try {
            val appid = update()
            try {
                val taskId = commit(appid)
                try {
                    val downloadUrl = query(taskId)
                    try {
                        println("开始下载")
                        download(downloadUrl)
                        println("下载成功")
                    } catch (e: Exception) {
                        println("下载失败")
                        e.printStackTrace()
                    }
                } catch (e: Exception){
                    println("加固失败")
                    e.printStackTrace()
                }
            } catch (e: Exception){
                println("提交加固失败")
                e.printStackTrace()
            }
        } catch (e: Exception){
            println("上传失败")
            e.printStackTrace()
        }
    }

    /**
     * 下载文件
     */
    private fun download(url: String){
        val request = Request.Builder()
            .url(url)
            .get()
            .headers(getHeader())
            .build()
        val response = http().newCall(request).execute()
        writeFile(response.body?.byteStream())
    }

    /**
     * 写入文件
     */
    private fun writeFile(input: InputStream?){
        if (input == null) {
            println("下载失败")
            return
        }
        var outputStream: OutputStream? = null
        try {
            val file = File("${jiaguParams.saveFilePath}/${apkFile.name}")
            createOrExistsFile(file)
            outputStream = FileOutputStream(file)
            var len: Int
            val buffer = ByteArray(1024 * 10)
            while (input.read(buffer).also { len = it } != -1) {
                outputStream.write(buffer, 0, len)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                input.close()
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun createOrExistsFile(file: File) : Boolean {
        if (file.exists()) return file.isFile
        if (!createOrExistsDir(file.parentFile)) return false
        return file.createNewFile()
    }

    private fun createOrExistsDir(file: File): Boolean {
        return if (file.exists()) file.isDirectory else file.mkdirs()
    }

    /**
     * 查询加固结果
     */
    private fun query(taskId: Int): String {
        try {
            val request = Request.Builder()
                .url("https://wm-shy.must.edu.mo/api/client_api/protect-result/")
                .headers(getHeader())
                .post("{\"taskid\":${taskId}}".toRequestBody())
                .build()
            val response = http().newCall(request).execute()
            val body = response.body?.string()
            println("查询加固结果：")
            println(body)
            val json = JsonParser().parse(body).asJsonObject
            return when (json.get("code").asInt) {
                0 -> {
                    println("加固成功")
                    json.get("downurl").asString
                }
                800000 -> {
                    Thread.sleep(3000)
                    query(taskId)
                }
                else -> {
                    throw Exception("加固失败")
                }
            }
        } catch (e: Exception) {
            Thread.sleep(3000)
            return query(taskId)
        }
    }

    /**
     * 提交加固
     */
    private fun commit(appid: Int): Int{
        val request = Request.Builder()
            .url("https://wm-shy.must.edu.mo/api/client_api/protect/")
            .headers(getHeader())
            .post("{\"appid\": \"$appid\", \"protect_tool\": 1, \"sid\": \"${jiaguParams.sid}\"}".toRequestBody())
            .build()
        val response = http().newCall(request).execute()
        val body = response.body?.string()
        println("提交加固：")
        println(body)
        return JsonParser().parse(body).asJsonObject.get("task_id").asInt
    }

    /**
     * 文件上传
     */
    private fun update(): Int {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        bodyBuilder.addFormDataPart("file", apkFile.name, apkFile.asRequestBody())
        val request = Request.Builder().url("https://wm-shy.must.edu.mo/api/client_api/upload/")
            .headers(getHeader())
            .post(bodyBuilder.build())
            .build()
        val response = http().newCall(request).execute()
        val body = response.body?.string()
        println("上传：")
        println(body)
        return JsonParser().parse(body).asJsonObject.get("appid").asInt
    }

    private fun http(): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getHeader() :Headers {
        val apiKey = "64e1b8d34f425d19e1ee2ea7236d3028"
        val apiSecret = "8d34f425"
        val nonce = UUID.randomUUID().toString()
        val curTime = System.currentTimeMillis()
        val checkSum = encryptToSHA(apiSecret + nonce + curTime.toString())
        return Headers.Builder().add("Nonce", nonce)
            .add("ApiKey", apiKey)
            .add("CurTime", curTime.toString())
            .add("CheckSum", checkSum)
            .build()
    }

    private fun encryptToSHA(info: String) : String{
        val alga = MessageDigest.getInstance("SHA-1")
        alga.update(info.toByteArray())
        val digest = alga.digest()
        return byte2hex(digest)
    }

    private fun byte2hex(b: ByteArray): String {
        var hs = ""
        var stmp: String
        b.forEach {
            stmp = Integer.toHexString(it.and(0XFF))
            if (stmp.length == 1) {
                hs = hs + "0" + stmp
            } else {
                hs += stmp
            }
        }
        return hs
    }
}