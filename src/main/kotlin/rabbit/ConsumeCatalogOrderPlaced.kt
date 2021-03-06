package rabbit

import com.google.gson.annotations.SerializedName
import model.article.ArticleRepository
import utils.errors.ValidationError
import utils.gson.jsonToObject
import utils.rabbit.RabbitEvent
import utils.rabbit.TopicConsumer
import utils.validator.Required
import utils.validator.validate

object ConsumeCatalogOrderPlaced {
    fun init() {
        TopicConsumer("sell_flow", "topic_catalog", "order_placed").apply {
            addProcessor("order-placed") { e: RabbitEvent? -> processOrderPlaced(e) }
            start()
        }
    }

    /**
     *
     * @api {topic} order/order-placed Orden Creada
     *
     * @apiGroup RabbitMQ
     *
     * @apiDescription Consume de mensajes order-placed desde Order con el topic "order_placed".
     *
     * @apiSuccessExample {json} Mensaje
     * {
     * "type": "order-placed",
     * "message" : {
     * "cartId": "{cartId}",
     * "orderId": "{orderId}"
     * "articles": [{
     * "articleId": "{model.article id}"
     * "quantity" : {quantity}
     * }, ...]
     * }
     * }
     */
    private fun processOrderPlaced(event: RabbitEvent?) {
        event?.message?.toString()?.jsonToObject<OrderPlacedEvent>()?.let {
            try {
                println("RabbitMQ Consume order-placed : " + it.orderId)
                it.validate()
                it.articles.forEach { a ->
                    try {
                        val article = ArticleRepository.findById(a.articleId).value()

                        val data = EventArticleData(
                            articleId = article.id,
                            price = article.price,
                            referenceId = it.orderId,
                            stock = article.stock,
                            valid = article.enabled
                        )

                        EmitArticleData.sendArticleData(event, data)
                    } catch (validation: ValidationError) {
                        val data = EventArticleData(
                            articleId = a.articleId,
                            referenceId = it.orderId,
                            valid = false
                        )
                        EmitArticleData.sendArticleData(event, data)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    private data class OrderPlacedEvent(
        @SerializedName("orderId")
        var orderId: String? = null,

        @SerializedName("cartId")
        val cartId: String? = null,

        @SerializedName("articles")
        val articles: Array<Article> = emptyArray()
    ) {

        data class Article(
            @SerializedName("articleId")
            @Required
            val articleId: String? = null,

            @SerializedName("quantity")
            @Required
            val quantity: Int = 0
        )
    }

}