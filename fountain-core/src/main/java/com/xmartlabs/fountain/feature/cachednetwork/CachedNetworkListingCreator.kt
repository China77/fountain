package com.xmartlabs.fountain.feature.cachednetwork

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.xmartlabs.fountain.ListResponse
import com.xmartlabs.fountain.Listing
import com.xmartlabs.fountain.adapter.BaseNetworkDataSourceAdapter
import com.xmartlabs.fountain.adapter.CachedDataSourceAdapter
import java.util.concurrent.Executor

object CachedNetworkListingCreator {
  @Suppress("LongParameterList")
  fun <NetworkValue, DataSourceValue, ServiceResponse : ListResponse<out NetworkValue>> createListing(
      cachedDataSourceAdapter: CachedDataSourceAdapter<NetworkValue, DataSourceValue>,
      firstPage: Int,
      ioDatabaseExecutor: Executor,
      ioServiceExecutor: Executor,
      pagedListConfig: PagedList.Config,
      networkDataSourceAdapter: BaseNetworkDataSourceAdapter<ServiceResponse>
  ): Listing<DataSourceValue> {

    val boundaryCallback = BoundaryCallback(
        networkDataSourceAdapter = networkDataSourceAdapter,
        firstPage = firstPage,
        cachedDataSourceAdapter = cachedDataSourceAdapter,
        pagedListConfig = pagedListConfig,
        ioDatabaseExecutor = ioDatabaseExecutor,
        ioServiceExecutor = ioServiceExecutor
    )

    val builder = LivePagedListBuilder(cachedDataSourceAdapter.getDataSourceFactory(), pagedListConfig)
        .setBoundaryCallback(boundaryCallback)

    val refreshTrigger = MutableLiveData<Unit>()
    val refreshState = Transformations.switchMap(refreshTrigger) {
      boundaryCallback.resetData()
    }

    return Listing(
        pagedList = builder.build(),
        networkState = boundaryCallback.networkState,
        retry = {
          boundaryCallback.retry()
        },
        refresh = {
          refreshTrigger.value = null
        },
        refreshState = refreshState
    )
  }
}
