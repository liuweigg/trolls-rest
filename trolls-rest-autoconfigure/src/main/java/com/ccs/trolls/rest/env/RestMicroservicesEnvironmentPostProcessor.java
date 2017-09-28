package com.ccs.trolls.rest.env;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

//参考实现：https://github.com/spring-projects/spring-boot/blob/v1.4.2.RELEASE/spring-boot-devtools/src/main/java/org/springframework/boot/devtools/env/DevToolsPropertyDefaultsPostProcessor.java
//以下程序提供我们基于 Spring Cloud 开发 Rest 微服务应用的公共默认设置，但是可以被大多数如命令行参数、环境变量、应用配置文件、Config Server 等多种来源覆盖。
//其中有些配置可能在实际应用中并未用到相关功能组件，但是参照 DevToolsPropertyDefaultsPostProcessor 的情况，设置了这些属性也无所谓。

//注意对 Profile 的命名有约束（大小写敏感）：
//  环境区分：development、test*（testCi/testUat/test1/test2 都可）、production。
//  是否启用：discovery（启用服务注册及发现）、config（启用集中配置管理）、cloud（前两者都启用）
//用法：set spring.profiles.active=development 或者 set spring.profiles.active=development,discovery 等。

@Order(Ordered.LOWEST_PRECEDENCE)
public class RestMicroservicesEnvironmentPostProcessor implements EnvironmentPostProcessor {
  private static final String DEFAULT_PROPERTIES = "defaultProperties";

  private static final String DEFAULT_USER = "admin";
  private static final String DEFAULT_PASSWORD_IN_DEV_ENV = "admin";

  private static final Map<String, Object> PROPERTIES_ALL_PROFILES;
  private static final Map<String, Object> PROPERTIES_DEV_PROFILE;
  private static final Map<String, Object> PROPERTIES_NON_DEV_PROFILES;

  static {
    //所有环境下的默认设置
    Map<String, Object> all = new HashMap<>();

    // 基本
    all.put("info.key", "${spring.application.name}");
    all.put("info.instance", "${trolls.appinstance.name}");
    all.put("management.context-path", "/admin"); //所有运维支持类的 Rest 访问统一到 admin 路径下。
    all.put(
        "trolls.appinstance.name",
        "${spring.application.name}.${spring.cloud.client.hostname:${HOSTNAME:hostname}}.${server.port:8080}"); //应用实例名，主要用在日志文件名称中。

    // 日志
    all.put("logging.file", "${trolls.log.directory}/${trolls.appinstance.name}.log");
    all.put("server.tomcat.accesslog.enabled", true);
    all.put("server.tomcat.accesslog.directory", "${trolls.log.directory}");
    all.put("server.tomcat.accesslog.prefix", "${trolls.appinstance.name}.access");

    // 安全
    all.put("endpoints.info.sensitive", false);
    all.put("endpoints.health.sensitive", false);
    all.put("security.user.name", DEFAULT_USER);

    // 服务发现/Eureka
    all.put("eureka.instance.hostname", "${spring.cloud.client.hostname}");
    all.put("eureka.instance.ipAddress", "${spring.cloud.client.hostname}");
    all.put("eureka.instance.statusPageUrlPath", "${management.context-path}/info");
    all.put("eureka.instance.healthCheckUrlPath", "${management.context-path}/health");

    // json 转换
    all.put(
        "spring.jackson.serialization.WRITE_ENUMS_USING_TO_STRING",
        true); //Enum类型在生成json数据时不会全部转成enum名称大写，而完全取决于api文档的定义。
    all.put("spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS", false);

    // 其他
    all.put("spring.datasource.sql-script-encoding", "UTF-8");

    PROPERTIES_ALL_PROFILES = Collections.unmodifiableMap(all);

    //开发环境下的默认设置
    Map<String, Object> dev = new HashMap<>();

    dev.put("eureka.instance.leaseRenewalIntervalInSeconds", 5); //仅在开发环境调整方便快速响应
    dev.put(
        "security.user.password",
        DEFAULT_PASSWORD_IN_DEV_ENV); //此处设置默认密码仅仅是为了在开发环境验证安全功能是否生效，在其他环境需加密保存。
    dev.put("spring.h2.console.enabled", true);
    dev.put("spring.jpa.show-sql", true);
    dev.put("spring.sleuth.sampler.percentage", "1.0");
    dev.put("trolls.log.directory", "${user.dir}/target"); //这样在开发时可以简单的通过 "maven clean" 删除日志文件。

    PROPERTIES_DEV_PROFILE = Collections.unmodifiableMap(dev);

    //非开发环境下的默认设置
    Map<String, Object> nondev = new HashMap<>();
    nondev.put(
        "security.user.password",
        "Change_And_Encrypt_It_Or_Override_It_In_Env"); //非开发环境的密码必须加密保存在 Config Server 或设置为环境变量或由 PASS 等平台管理。
    nondev.put("trolls.log.directory", "${user.dir}/logs");
    PROPERTIES_NON_DEV_PROFILES = Collections.unmodifiableMap(nondev);
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String env = null;
    boolean enableDiscoveryClient = false;
    boolean enableConfigClient = false;
    for (String s : environment.getActiveProfiles()) {
      if ("development".equals(s) || "production".equals(s)) env = s;
      else if (s.startsWith("test")) env = "test";
      //if spring.profiles.active=development,test,production, then last one wins.
      else if ("discovery".equals(s)) enableDiscoveryClient = true;
      else if ("config".equals(s)) enableConfigClient = true;
      else if ("cloud".equals(s)) {
        enableDiscoveryClient = true;
        enableConfigClient = true;
      }
    }
    boolean isDevEnv = "development".equals(env);

    Map<String, Object> more = new HashMap<>();
    more.put("spring.cloud.discovery.enabled", enableDiscoveryClient);
    more.put("spring.cloud.config.discovery.enabled", enableConfigClient);
    more.put("spring.cloud.config.enabled", enableConfigClient);
    if (enableConfigClient) {
      more.put("spring.cloud.config.failFast", true);
      if (isDevEnv) {
        more.put("spring.cloud.config.username", DEFAULT_USER);
        more.put("spring.cloud.config.password", DEFAULT_PASSWORD_IN_DEV_ENV);
      }
    }

    Map<String, Object> props = new HashMap<>();
    props.putAll(PROPERTIES_ALL_PROFILES);
    if (isDevEnv) props.putAll(PROPERTIES_DEV_PROFILE);
    else props.putAll(PROPERTIES_NON_DEV_PROFILES);
    props.putAll(more);

    //PropertySource<?> source = new MapPropertySource(DEFAULT_PROPERTIES, props); //如果设置到 DEFAULT_PROPERTIES 中去则不能象下面这样直接做，需按以前类似 sleuth TraceEnvironmentPostProcessor 的复杂做法；但按以下设置另一个 Property Source，则还是比 DEFAULT_PROPERTIES 优先级要高（问题不大，会被大量其他高优先级的来源覆盖）。
    PropertySource<?> source = new MapPropertySource("trollsConfig", props);
    environment.getPropertySources().addLast(source);
  }
}
