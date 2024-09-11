# AutoDDNS (ADDNS)

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](https://travis-ci.org/yourusername/AutoDDNS)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![GitHub release](https://img.shields.io/github/release/yourusername/AutoDDNS.svg)](https://github.com/yourusername/AutoDDNS/releases/latest)

AutoDDNS（简称ADDNS）是一个动态域名解析系统更新工具，旨在帮助用户自动更新其域名解析记录，以适应动态IP地址环境。本项目采用Java 17编写，并利用GraalVM技术编译成本地可执行文件，支持IPv4和IPv6双栈环境。

## 特点

- **高效性**：快速响应网络地址变化。
- **兼容性**：支持多种DNS服务提供商API（当前仅支持火山引擎TrafficRoute DNS套件）。
- **轻量化**：使用GraalVM编译为本地镜像，减少资源消耗。
- **易扩展**：通过简单的代码修改即可添加对其他DNS服务商的支持。

## 快速开始

### 安装与配置

1. 下载最新版本的AutoDDNS从[发布页面](https://github.com/yourusername/AutoDDNS/releases/latest)。
2. 解压下载的包到指定目录。
3. 配置你的域名解析服务提供商的相关API密钥和访问令牌。
4. 运行AutoDDNS，它会自动检测当前网络接口的IP地址并更新至DNS服务器。

### 扩展支持

要支持新的DNS服务提供商，请按照以下步骤操作：

1. 在`src/main/java/com/auto/ddns`目录下创建新的API适配器类。
2. 实现必要的方法来调用新服务提供商的API接口。
3. 更新`main`函数或其他启动点，以便在运行时选择正确的适配器。

## 贡献指南

欢迎任何人贡献代码或提出改进建议。请遵循以下流程：

1. Fork本仓库。
2. 创建一个新分支进行开发。
3. 提交更改前确保所有测试均通过。
4. 发起一个Pull Request。

## 许可证

AutoDDNS项目遵循Apache License 2.0许可协议。更多信息参见[LICENSE](LICENSE)文件。

---

**注意：** 本项目仅为学习交流目的，不得用于任何非法活动或侵犯他人权益的行为。
