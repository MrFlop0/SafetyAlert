package alex.kaplenkov.safetyalert.data.api

import alex.kaplenkov.safetyalert.data.api.model.request.CreateOrderRequest
import alex.kaplenkov.safetyalert.data.api.model.request.UploadPhotoRequest
import alex.kaplenkov.safetyalert.data.api.model.response.OrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/v1/integration/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>

    @POST("api/v1/integration/orders/photo")
    suspend fun uploadPhoto(@Body request: UploadPhotoRequest): Response<Any>
}