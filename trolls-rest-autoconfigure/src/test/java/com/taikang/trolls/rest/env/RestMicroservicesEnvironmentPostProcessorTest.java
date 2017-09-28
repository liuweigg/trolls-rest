package com.taikang.trolls.rest.env;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.ccs.trolls.rest.env.RestMicroservicesEnvironmentPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

public class RestMicroservicesEnvironmentPostProcessorTest {

  private RestMicroservicesEnvironmentPostProcessor processor =
      new RestMicroservicesEnvironmentPostProcessor();

  private ConfigurableEnvironment environment = new StandardEnvironment();

  private static String APPLICATION_NAME = "MyTrollsApplication";
  private static String HOSTNAME = "MyHostname";
  private static int PORT_NUMBER = 8080;

  /** 测试：开发环境下的配置信息 */
  @Test
  public void developmentProperties() {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=development");

    this.processor.postProcessEnvironment(this.environment, null);

    String userDir = this.environment.resolvePlaceholders("${user.dir}");

    this.checkPropertisForAllEnvironment();
    assertThat(
            this.environment.resolvePlaceholders(
                "${eureka.instance.leaseRenewalIntervalInSeconds}"))
        .isEqualTo("5");
    assertThat(this.environment.resolvePlaceholders("${security.user.password}"))
        .isEqualTo("admin");
    assertThat(this.environment.resolvePlaceholders("${spring.h2.console.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.jpa.show-sql}")).isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.sleuth.sampler.percentage}"))
        .isEqualTo("1.0");
    assertThat(this.environment.resolvePlaceholders("${trolls.log.directory}"))
        .isEqualTo(userDir + "/target");
  }

  /** 测试：非开发环境下的配置信息 */
  @Test
  public void nondevelopmentProperties() {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=test");

    this.processor.postProcessEnvironment(this.environment, null);

    String userDir = this.environment.resolvePlaceholders("${user.dir}");

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${security.user.password}"))
        .isEqualTo("Change_And_Encrypt_It_Or_Override_It_In_Env");
    assertThat(this.environment.resolvePlaceholders("${trolls.log.directory}"))
        .isEqualTo(userDir + "/logs");
  }

  /** 测试仅启用 discovery profile 而不启用 config profile 时配置信息 */
  @Test
  public void discoveryProfileOnly() {

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=discovery");

    this.processor.postProcessEnvironment(this.environment, null);

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.discovery.enabled}"))
        .isEqualTo("true");
  }

  /** 测试开发环境下仅启用 config profile 而不启用 discovery profile 时配置信息 */
  @Test
  public void configProfileOnlyAndDevelopmentEnvironment() {

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=development,config");

    this.processor.postProcessEnvironment(this.environment, null);

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.discovery.enabled}"))
        .isEqualTo("false");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.failFast}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.username}"))
        .isEqualTo("admin");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.password}"))
        .isEqualTo("admin");
  }

  /** 测试非开发环境下仅启用 config profile 而不启用 discovery profile 时配置信息 */
  @Test
  public void configProfileOnlyAndNondevelopmentEnvironment() {

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=test,config");

    this.processor.postProcessEnvironment(this.environment, null);

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.discovery.enabled}"))
        .isEqualTo("false");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.failFast}"))
        .isEqualTo("true");
    //当使用resolvePlaceholders()提取的配置不存时，会返回参数名。
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.username}"))
        .isEqualTo("${spring.cloud.config.username}");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.password}"))
        .isEqualTo("${spring.cloud.config.password}");
  }

  /** 测试开发环境下启用 cloud profile 时配置信息 */
  @Test
  public void cloudProfileAndDevelopmentEnvironment() {

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=development,cloud");

    this.processor.postProcessEnvironment(this.environment, null);

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.failFast}"))
        .isEqualTo("true");
    //当使用resolvePlaceholders()提取的配置不存时，会返回参数名。
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.username}"))
        .isEqualTo("admin");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.password}"))
        .isEqualTo("admin");
  }

  /** 测试非开发环境下启用 cloud profile 时配置信息 */
  @Test
  public void cloudProfileAndNondevelopmentEnvironment() {

    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.application.name=" + this.APPLICATION_NAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.cloud.client.hostname=" + this.HOSTNAME);
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        this.environment, "spring.profiles.active=cloud");

    this.processor.postProcessEnvironment(this.environment, null);

    this.checkPropertisForAllEnvironment();
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.discovery.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.failFast}"))
        .isEqualTo("true");
    //当使用resolvePlaceholders()提取的配置不存时，会返回参数名。
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.username}"))
        .isEqualTo("${spring.cloud.config.username}");
    assertThat(this.environment.resolvePlaceholders("${spring.cloud.config.password}"))
        .isEqualTo("${spring.cloud.config.password}");
  }

  /** 测试：所有环境下的共通配置信息 */
  private void checkPropertisForAllEnvironment() {
    String trollsLogDir = this.environment.resolvePlaceholders("${trolls.log.directory}");
    String trollsAppinstanceName =
        this.environment.resolvePlaceholders("${trolls.appinstance.name}");
    String managementContextPath =
        this.environment.resolvePlaceholders("${management.context-path}");
    // 基本
    assertThat(this.environment.resolvePlaceholders("${info.key}"))
        .isEqualTo(this.APPLICATION_NAME);
    assertThat(this.environment.resolvePlaceholders("${info.instance}"))
        .isEqualTo(trollsAppinstanceName);
    assertThat(this.environment.resolvePlaceholders("${management.context-path}"))
        .isEqualTo("/admin");
    assertThat(this.environment.resolvePlaceholders("${trolls.appinstance.name}"))
        .isEqualTo(this.APPLICATION_NAME + "." + this.HOSTNAME + "." + PORT_NUMBER);

    // 日志
    assertThat(this.environment.resolvePlaceholders("${logging.file}"))
        .isEqualTo(trollsLogDir + "/" + trollsAppinstanceName + ".log");
    assertThat(this.environment.resolvePlaceholders("${server.tomcat.accesslog.enabled}"))
        .isEqualTo("true");
    assertThat(this.environment.resolvePlaceholders("${server.tomcat.accesslog.directory}"))
        .isEqualTo(trollsLogDir);
    assertThat(this.environment.resolvePlaceholders("${server.tomcat.accesslog.prefix}"))
        .isEqualTo(trollsAppinstanceName + ".access");

    // 安全
    assertThat(this.environment.resolvePlaceholders("${endpoints.info.sensitive}"))
        .isEqualTo("false");
    assertThat(this.environment.resolvePlaceholders("${endpoints.health.sensitive}"))
        .isEqualTo("false");
    assertThat(this.environment.resolvePlaceholders("${security.user.name}")).isEqualTo("admin");

    // 服务发现/Eureka
    assertThat(this.environment.resolvePlaceholders("${eureka.instance.hostname}"))
        .isEqualTo(this.HOSTNAME);
    assertThat(this.environment.resolvePlaceholders("${eureka.instance.ipAddress}"))
        .isEqualTo(this.HOSTNAME);
    assertThat(this.environment.resolvePlaceholders("${eureka.instance.statusPageUrlPath}"))
        .isEqualTo(managementContextPath + "/info");
    assertThat(this.environment.resolvePlaceholders("${eureka.instance.healthCheckUrlPath}"))
        .isEqualTo(managementContextPath + "/health");

    // json 转换
    assertThat(
            this.environment.resolvePlaceholders(
                "${spring.jackson.serialization.WRITE_ENUMS_USING_TO_STRING}"))
        .isEqualTo("true");
    assertThat(
            this.environment.resolvePlaceholders(
                "${spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS}"))
        .isEqualTo("false");

    // 其他
    assertThat(this.environment.resolvePlaceholders("${spring.datasource.sql-script-encoding}"))
        .isEqualTo("UTF-8");
  }
}
