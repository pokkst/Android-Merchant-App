package com.bitcoin.merchant.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import org.bitcoindotcom.bchprocessor.bip70.GsonHelper.gson
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * Implementation of AsyncTask that runs a network operation on a background thread.
 */
abstract class DownloadTask<R>(private val activity: Context) {
    private val activeNetworkInfo: NetworkInfo
        private get() {
            val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return connectivityManager.activeNetworkInfo
        }

    // If no connectivity, cancel task and update Callback with null data.
    protected val isCancelled: Boolean
        protected get() {
            val networkInfo = activeNetworkInfo
            // If no connectivity, cancel task and update Callback with null data.
            return networkInfo == null || !networkInfo.isConnected ||
                    (networkInfo.type != ConnectivityManager.TYPE_WIFI
                            && networkInfo.type != ConnectivityManager.TYPE_MOBILE)
        }

    protected abstract fun onDownloaded(result: R?)
    fun execute() {
        val url = url
        if (url == null || isCancelled) {
            try {
                onDownloaded(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        val thread: Thread = object : Thread(url) {
            override fun run() {
                var r: R? = null
                try {
                    try {
                        r = download()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } finally {
                    try {
                        onDownloaded(r)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        thread.isDaemon = true
        thread.name = url
        thread.start()
    }

    protected fun download(): R? {
        var result: R? = null
        if (!isCancelled) {
            result = try {
                val url = URL(url)
                val resultString = downloadUrl(url)
                if (resultString != null) {
                    gson.fromJson(resultString, returnClass)
                } else {
                    throw IOException("No response received.")
                }
            } catch (e: Exception) {
                null
            }
        }
        return result
    }

    protected abstract val returnClass: Class<R>?
    protected abstract val url: String?
    /**
     * Given a URL, sets up a connection and gets the HTTP response body from the server.
     * If the network request is successful, it returns the response body in String form. Otherwise,
     * it will throw an IOException.
     */
    @Throws(IOException::class)
    private fun downloadUrl(url: URL): String? {
        var stream: InputStream? = null
        var connection: HttpsURLConnection? = null
        var result: String? = null
        try {
            Log.i("DownloadTask", url.toString())
            connection = url.openConnection() as HttpsURLConnection
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection!!.readTimeout = 3000
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.connectTimeout = 3000
            // For this use case, set HTTP method to GET.
            connection.requestMethod = "GET"
            // Already true by default but setting just in case; needs to be true since this request
// is carrying an input (response) body.
            connection.doInput = true
            // Open communications link (network traffic occurs here).
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: $responseCode")
            }
            // Retrieve the response body as an InputStream.
            stream = connection.inputStream
            if (stream != null) { // Converts Stream to String
                result = readStream(stream)
            }
        } finally { // Close Stream and disconnect HTTPS connection.
            stream?.close()
            connection?.disconnect()
        }
        return result
    }

    companion object {
        @Throws(IOException::class)
        fun readStream(stream: InputStream): String {
            val tempBuffer = ByteArray(65536)
            val bytes = loadBytes(stream, tempBuffer)
            return String(bytes, StandardCharsets.UTF_8)
        }

        @Throws(IOException::class)
        fun loadBytes(`is`: InputStream, tempBuffer: ByteArray?): ByteArray {
            val os = ByteArrayOutputStream()
            copy(os, `is`, tempBuffer)
            return os.toByteArray()
        }

        @Throws(IOException::class)
        fun copy(os: OutputStream, `is`: InputStream, tempBuffer: ByteArray?): Int {
            var total = 0
            while (true) {
                val read = `is`.read(tempBuffer)
                if (read == -1) { // end of file reached
                    break
                }
                os.write(tempBuffer, 0, read)
                total += read
            }
            return total
        }
    }

}