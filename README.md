# AutoDDNS (ADDNS)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

AutoDDNS（简称ADDNS）是一个动态域名解析系统更新工具，旨在帮助用户自动更新其域名解析记录，以适应动态IP地址环境。本项目采用 Java 17 编写，并利用 GraalVM 技术编译成本地可执行文件，支持 IPv4 和 IPv6 双栈环境。

## 特点

- **轻量化**：使用 GraalVM 编译为本地镜像，减少资源消耗。
- **易扩展**：通过简单的代码修改即可添加对其他 DNS API 服务商的支持。
- **无内置定时监控**：AutoDDNS 本身不提供定时监控功能，用户可以根据自身需求选择合适的定时任务调度方式。每次运行 AutoDDNS 时，它会自动扫描当前网络接口的 IP 地址，并更新至 DNS 服务器，保持系统的简洁性。
- **跨平台支持**：由于采用 Java 编写，并且通过 GraalVM 编译为本地镜像，理论上只要 GraalVM 支持的运行环境，AutoDDNS 就可以编译并在该环境下使用。（目前仅提供 Windows 编译程序，其他平台需自行编译）

## 快速开始

### 安装与配置

1. 下载最新版本的 ADDNS 从[发布页面](https://github.com/yahocen/AutoDDNS/releases/latest)。
2. 解压下载的包到指定目录。
3. 修改配置文件，参考[配置文件说明](doc/配置文件说明.md)。
4. 运行 ADDNS，它会自动检测当前网络接口的IP地址并更新至DNS服务器。
5. 根据系统选择合适任务调度方式，参考（[Windows任务调度方式](doc/Windows任务调度方式.md)，[Linux任务调度方式](doc/Windows任务调度方式.md)）

### 扩展支持

要支持新的DNS服务提供商，请按照以下步骤操作：

1. 在`src/main/java/org/addns/dns`目录下创建新的API适配器类，并继承`org.addns.dns.DnsOper`。
2. 实现必要的方法来调用新服务提供商的API接口。
3. 更新`main`函数或其他启动点，以便在运行时加载正确的适配器。

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
