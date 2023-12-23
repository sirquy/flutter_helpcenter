package com.zendesk_helpcenter

import android.util.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import zendesk.core.JwtIdentity
import zendesk.core.Zendesk
import zendesk.support.CustomField
import zendesk.support.RequestProvider
import zendesk.support.Support
import zendesk.support.guide.HelpCenterActivity
import zendesk.support.request.RequestActivity
import zendesk.support.requestlist.RequestListActivity
import java.util.Arrays
import zendesk.configurations.Configuration
import zendesk.core.AnonymousIdentity
import zendesk.core.Identity
import zendesk.support.Request
import zendesk.support.CreateRequest
import zendesk.support.UploadProvider
import android.content.Intent

import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback

interface TicketsCallback {
    fun onTicketsReceived(tickets: List<Map<String, String>>)
    fun onError(errorMessage: String)
}

class FlutterZendeskCommonMethod (private val plugin: FlutterZendeskPlugin, private val channel: MethodChannel) {
    
    companion object {
        const val tag = "[ZendeskMessaging]"
        const val initializeSuccess: String = "initialize_success"
        val provider: RequestProvider = Support.INSTANCE.provider()!!.requestProvider()
    }

    private var ticketsCallback: TicketsCallback? = null

    fun setTicketsCallback(callback: TicketsCallback) {
        this.ticketsCallback = callback
    }

    fun initialize(
        urlString: String,
        appId: String,
        clientId: String,
        nameIdentifier: String,
    ) {

        Zendesk.INSTANCE.init(plugin.activity!!, urlString, appId, clientId)
        Zendesk.INSTANCE.setIdentity(JwtIdentity(nameIdentifier))
        Support.INSTANCE.init(Zendesk.INSTANCE)

        plugin.isInitialize = true

        channel.invokeMethod(initializeSuccess, null)
    }

    fun openHelpCenter(call: MethodCall) {
        
        var helpCenter = HelpCenterActivity.builder()
        .withContactUsButtonVisible(true)
        .withShowConversationsMenuButton(true)

        val groupType = call.argument<String?>("groupType")
        val groupIds = call.argument<List<Long>?>("groupIds")

        when(groupType) {
          "category" -> {
            helpCenter = helpCenter.withArticlesForCategoryIds(groupIds!!)
          }
          "section" -> {
            helpCenter = helpCenter.withArticlesForSectionIds(groupIds!!)
          }
        }

        helpCenter.show(plugin.activity!!)
    }

    fun getTickets(call: MethodCall) {
        // ... (các phần khác)

        provider.getAllRequests(object : ZendeskCallback<List<Request>>() {
            override fun onSuccess(listRequests: List<Request>) {
                val resultDataTyped: List<Map<String, String>> = listRequests.mapNotNull { request ->
                    // Chuyển đổi và loại bỏ giá trị null ngay tại đây
                    mapOf(
                        "requestId" to (request.id?.toString() ?: ""), // Kiểm tra nếu id null thì sử dụng giá trị mặc định là ""
                        "subject" to (request.subject ?: "") // Kiểm tra nếu subject null thì sử dụng giá trị mặc định là ""
                    )
                }

                // Gọi callback để truyền dữ liệu về FlutterZendeskPlugin
                ticketsCallback?.onTicketsReceived(resultDataTyped)
            }

            override fun onError(errorResponse: ErrorResponse) {
                Log.d(tag, "onError: " + errorResponse.reason)
                // Gọi callback để truyền thông báo lỗi về FlutterZendeskPlugin
                ticketsCallback?.onError(errorResponse.reason?.toString() ?: "") // Kiểm tra nếu reason null thì sử dụng giá trị mặc định là ""
            }
        })
    }




    fun showTicket(call: MethodCall) {
        val requestActivityIntent = RequestActivity.builder()
            .intent(plugin.activity!!)

        plugin.activity?.startActivity(requestActivityIntent)
    }

    fun newTicket(call: MethodCall) {
        

        val createRequest = CreateRequest()
        createRequest.subject = "subject"
        createRequest.description = "description"

        // Call the provider method
        provider.createRequest(createRequest, requestCallback())
    }

    private fun requestCallback(): ZendeskCallback<Request> {
        return object : ZendeskCallback<Request>() {
            override fun onSuccess(createRequest: Request) {
                // Handle success
                Log.d(tag, "onSuccess: Ticket created!")
                // You can send a success message back to Dart here if needed
            }

            override fun onError(errorResponse: ErrorResponse) {
                // Handle error
                Log.d(tag, "onError: " + errorResponse.reason)
                // You can send an error message back to Dart here if needed
            }
        }
    }

}