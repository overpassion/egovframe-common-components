package egovframework.com.cmm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.egovframe.rte.fdl.cmmn.message.EgovWildcardMessageSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;

/**
 * context-common.xml 의 messageSource 빈이 공통컴포넌트 사본(EgovWildcardReloadableResourceBundleMessageSource,
 * egovBasenames)에서 실행환경 {@link EgovWildcardMessageSource}(basenames) 로 교체된 뒤에도 메시지가 그대로 잡히는지
 * 설정 파일 기준으로 고정한다.
 *
 * <p>전체 컨텍스트를 띄우지 않고 빈 정의만 읽어 messageSource 하나만 생성한다(DB 불필요).</p>
 */
class EgovMessageSourceConfigTest {

	private static DefaultListableBeanFactory beanFactory() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ClassPathResource("egovframework/spring/com/context-common.xml"));
		return factory;
	}

	@Test
	@DisplayName("messageSource 는 실행환경 EgovWildcardMessageSource 다")
	void beanClassIsRuntimeWildcardMessageSource() {
		DefaultListableBeanFactory factory = beanFactory();

		assertEquals(EgovWildcardMessageSource.class.getName(), factory.getBeanDefinition("messageSource").getBeanClassName());
		assertTrue(factory.getBean("messageSource") instanceof EgovWildcardMessageSource);
	}

	@Test
	@DisplayName("와일드카드 패턴으로 최상위·하위 디렉터리의 메시지가 모두 잡힌다")
	void wildcardPatternCoversNestedDirectories() {
		MessageSource messageSource = beanFactory().getBean("messageSource", MessageSource.class);

		assertEquals("에러가 발생했습니다!", messageSource.getMessage("fail.common.msg", null, Locale.KOREAN));
		assertEquals("이전 페이지", messageSource.getMessage("comCmmErr.button", null, Locale.KOREAN));
	}

	@Test
	@DisplayName("패턴이 아닌 basename(실행환경 모듈 메시지·globals)은 그대로 전달된다")
	void plainBasenamesPassThrough() {
		MessageSource messageSource = beanFactory().getBean("messageSource", MessageSource.class);

		String idgnr = messageSource.getMessage("error.idgnr.not.supported", null, Locale.KOREAN);
		assertTrue(idgnr.startsWith("[IDGeneration Service]"), idgnr);
		assertNotNull(messageSource.getMessage("Globals.DbType", null, null, Locale.KOREAN));
	}

	@Test
	@DisplayName("영문 로케일도 같은 패턴으로 해석된다")
	void englishLocale() {
		MessageSource messageSource = beanFactory().getBean("messageSource", MessageSource.class);

		String message = messageSource.getMessage("fail.common.msg", null, Locale.ENGLISH);
		assertNotNull(message);
		assertTrue(!message.isEmpty());
	}
}
