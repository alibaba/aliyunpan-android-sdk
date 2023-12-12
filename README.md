

# AliyunpanSDK-Android

This is the open-source SDK for Aliyunpan-Android OpenAPI.

[![](https://jitpack.io/v/alibaba/aliyunpan-android-sdk.svg)](https://jitpack.io/#alibaba/aliyunpan-android-sdk)

## Getting Started


```
dependencies {
    implementation 'com.github.alibaba:aliyunpan-android-sdk:v0.0.3'
}
```


To begin using the sdk, visit our guide that will walk you through the setup process:

[Guide](https://www.yuque.com/aliyundrive/zpfszx/qkwg88uf4t483tdi)

## Quick start

### 1. Create client

```
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig


fun initApp(context: Context) {
        // recommend
        val config = AliyunpanClientConfig.Builder(context, "app_key")
            .build()

        // not recommend
        val config = AliyunpanClientConfig.Builder(context, "app_key")
            .appSecret("app_secret")
            .build()

        // init client
        aliyunpanClient = AliyunpanClient.init(config)
    }


```

### 2. Send Command


```
aliyunpanClient.send(AliyunpanUserScope.GetDriveInfo(),
            { result ->
                // success
            }, {
                // failure
            })
```

## Advanced Usage

### Kotlin


```

import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.kotlin.AliyunpanClient

fun initApp(context: Context) {
        // config
        val config = AliyunpanClientConfig.Builder(context, "app_key")
            .build()
        // init client
        val aliyunpanClient = AliyunpanClient.init(config)
    }
```


```
lifecycleScope.launch {
            try {
                val response = aliyunpanClient.send(AliyunpanUserScope.GetDriveInfo())
            } catch (e: Exception) {

            }
        }
```


## Requirements
- minSdk 21

## License

This project is licensed under the MIT License - see the [MIT License](LICENSE) file for details
