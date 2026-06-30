package com.orbis.orbis.repositories

import com.orbis.orbis.helpers.PrefManager
import com.orbis.orbis.models.subscriptions.*
import com.orbis.orbis.models.user.User
import com.orbis.orbis.network.ApiInterface
import io.reactivex.Single
import retrofit2.Response
import javax.inject.Inject

class SubscriptionRepositories @Inject constructor(
    private val apiInterface: ApiInterface,
    private val prefManager: PrefManager
) {
    fun createSubscription(
        groupKey: String,
        requestBody: CreateSubscriptionBody
    ): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.createSubscription(token, groupKey, requestBody)
    }

    fun editSubscription(
        groupKey: String,
        requestBody: CreateSubscriptionBody
    ): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.editSubscription(token, groupKey, requestBody)
    }

    fun deleteSubscription(
        groupKey: String,
        subscriptionKey: String,
    ): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deleteSubscription(token, groupKey, subscriptionKey)
    }

    fun getSubscriptionInfo(): Single<SubscriptionInfo> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getSubscriptionInfo(token)
    }

    fun createStripe(requestBody: CreateStripeBody): Single<CreateStripeResponse> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.createStripe(token, requestBody)
    }

    fun getStripe(): Single<GetStripeResponse> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getStripe(token)
    }

    fun updateStripe(): Single<CreateStripeResponse> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.updateStripe(token)
    }

    fun activateSubscription(groupKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.activateSubscription(token, groupKey)
    }

    fun deactivateSubscription(groupKey: String, sure: Boolean): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.deactivateSubscription(token, groupKey, sure)
    }

    fun getSubscriptions(groupKey: String, page: Int): Single<ArrayList<Subscription>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getSubscriptions(token, groupKey, page, 20)
    }

    fun getSubscribers(
        groupKey: String,
        subscriptionKey: String,
        page: Int
    ): Single<ArrayList<User>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getSubscribers(token, groupKey, subscriptionKey, page, 20)
    }

    fun getPurchases(
        groupKey: String,
        purchaseKey: String,
        page: Int
    ): Single<ArrayList<User>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPurchases(token, groupKey, purchaseKey, page, 20)
    }

    fun getSubscriptionStatistic(
        groupKey: String,
        subscriptionKey: String,
        type: String
    ): Single<SubscriptionStatistic> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getSubscriptionStatistic(token, groupKey, subscriptionKey, type)
    }

    fun getPurchaseKeyStatistic(
        groupKey: String,
        subscriptionKey: String,
        type: String
    ): Single<SubscriptionStatistic> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getPurchaseKeyStatistic(token, groupKey, subscriptionKey, type)
    }

    fun getMySubscriptions(): Single<ArrayList<Subscription>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMySubscriptions(token)
    }

    fun getMyPurchases(): Single<ArrayList<Subscription>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.getMyPurchases(token)
    }

    fun subscribeSubscription(subscriptionKey: String): Single<SubscribeResponse> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.subscribeSubscription(token, subscriptionKey)
    }

    fun purchaseSubscription(subscriptionKey: String, quantity: Int): Single<SubscribeResponse> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.purchaseSubscription(token, subscriptionKey, quantity)
    }

    fun unsubscribeSubscription(subscriptionKey: String): Single<Response<Void>> {
        var token = ""
        if (!prefManager.getIdToken().isNullOrEmpty()) {
            token = "Bearer " + prefManager.getIdToken()
        }
        return apiInterface.unsubscribeSubscription(token, subscriptionKey)
    }
}