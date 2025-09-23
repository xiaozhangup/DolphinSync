# DolphinSync

发行版本用于正常使用, 不含 TabooLib 本体。

# 项目介绍

Octopus 服务端配套数据同步插件  
支持所有玩家数据快速无感同步  
使用 MySQL + Redis 进行同步

```
./gradlew clean build
```

## 构建开发版本

开发版本包含 TabooLib 本体, 用于开发者使用, 但不可运行。

```
./gradlew clean taboolibBuildApi -PDeleteCode
```

> 参数 -PDeleteCode 表示移除所有逻辑代码以减少体积。
