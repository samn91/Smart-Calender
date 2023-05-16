package com.example.smartcalender

import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

val gson = Gson()

val client: OkHttpClient.Builder =
    OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)


val chatGptApi: ChatGpt =
    Retrofit.Builder().baseUrl("https://api.openai.com/v1/").client(client.build())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
        .build().create(ChatGpt::class.java)


interface ChatGpt {
    @POST("chat/completions")
    fun chat(
        @Header("Authorization") auth: String = AUTH, @Body body: RequestBody
    ): Single<ChatCompletion>
}

data class RequestBody(
    val model: String, val messages: List<Message>
) {
    data class Message(
        val role: String, val content: String
    )


    companion object {
        fun createRequestBody(message: String): RequestBody {
            val messages = arrayListOf(
                Message(
                    "assistant",
                    "fill the following: {title, note, summery, location, and date format: yyyyMMdd_HH:mm} in a json format"
                ), Message(
                    "user", message
                )
            )
            return RequestBody("gpt-3.5-turbo", messages)
        }
    }
}

data class ChatCompletion(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
) {
    data class Usage(
        val prompt_tokens: Int, val completion_tokens: Int, val total_tokens: Int
    )

    data class Choice(
        val message: Message, val finish_reason: String, val index: Int
    ) {
        data class Message(
            val role: String, val content: String
        ) {
            fun getCalenderEvent(): CalenderEvent? {
                val clearedContent = content.replace("\n", "").replace("\\", "")
                return gson.fromJson(clearedContent, CalenderEvent::class.java)
            }
        }
    }
}

data class CalenderEvent(
    val title: String, val note: String, val location: String, val date: String
)
