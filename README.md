

# AliyunpanSDK-Android

This is the open-source SDK for Aliyunpan-Android OpenAPI.

[![](https://jitpack.io/v/alibaba/aliyunpan-android-sdk.svg)](https://jitpack.io/#alibaba/aliyunpan-android-sdk)

## Getting Started


```
dependencies {
    implementation 'com.github.alibaba:aliyunpan-android-sdk:v0.2.2'
}
```


To begin using the sdk, visit our guide that will walk you through the setup process:

[Guide](https://www.yuque.com/aliyundrive/zpfszx/qkwg88uf4t483tdi)

## Quick start

### 1. Create client

#### PKCE (recommend)
```
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig


fun initApp(context: Context) {
        val config = AliyunpanClientConfig.Builder(context, "app_id")
            .build()
        // init client
        val aliyunpanClient = AliyunpanClient.init(config)
    }


```

#### Server

```
import com.alicloud.databox.opensdk.AliyunpanClient
import com.alicloud.databox.opensdk.AliyunpanClientConfig
import com.alicloud.databox.opensdk.AliyunpanTokenServer

val config = AliyunpanClientConfig.Builder(context, "app_id")
            .tokenServer(object : AliyunpanTokenServer {
                // implement some one
            })
            .build()
        // init client
        val aliyunpanClient = AliyunpanClient.init(config)
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

import com.alicloud.databox.opensdk.kotlin.AliyunpanClient

fun initApp(context: Context) {
        // config
        val config = AliyunpanClientConfig.Builder(context, "app_id")
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

### Download

```
aliyunpanClient.buildDownload(driveId, fileId, { task ->
            // success
            task.addStateChange { taskState ->
                when (taskState) {
                    BaseTask.TaskState.Abort -> {
                    }

                    is BaseTask.TaskState.Completed -> {
                    }

                    is BaseTask.TaskState.Failed -> {
                    }

                    is BaseTask.TaskState.Running -> {
                    }

                    BaseTask.TaskState.Waiting -> {
                    }
                }
            }
            val startResult = task.start()
        }, {
            // failure
        })
```

### Upload

```
aliyunpanClient.buildUpload(driveId, filePath, { task ->
            // success
            task.addStateChange { taskState ->
                when (taskState) {
                    BaseTask.TaskState.Abort -> {
                    }

                    is BaseTask.TaskState.Completed -> {
                    }

                    is BaseTask.TaskState.Failed -> {
                    }

                    is BaseTask.TaskState.Running -> {
                    }

                    BaseTask.TaskState.Waiting -> {
                    }
                }
            }
            val startResult = task.start()
        }, {
            // failure
        })
```


## Documents

[Documents](https://alibaba.github.io/aliyunpan-android-sdk/)

### About Repositories

Add it in your root build.gradle at the end of repositories:

```
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

## Requirements
- minSdk 21

## License

This project is licensed under the MIT License - see the [MIT License](LICENSE) file for details
