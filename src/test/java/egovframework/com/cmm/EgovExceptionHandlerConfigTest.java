package egovframework.com.cmm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.egovframe.rte.fdl.cmmn.exception.handler.EgovLoggingExceptionHandler;
import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * context-aspect.xml 의 예외 핸들러 빈이 공통컴포넌트 사본(EgovComExcepHndlr·EgovComOthersExcepHndlr)에서
 * 실행환경 {@link EgovLoggingExceptionHandler} 로 교체된 것을 설정 파일 기준으로 고정한다.
 *
 * <p>전체 컨텍스트를 띄우지 않고 빈 정의만 읽어 해당 빈 두 개만 생성한다(DB 불필요).</p>
 */
class EgovExceptionHandlerConfigTest {

	private static DefaultListableBeanFactory beanFactory() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ClassPathResource("egovframework/spring/com/context-aspect.xml"));
		return factory;
	}

	@Test
	@DisplayName("egovHandler·otherHandler 는 실행환경 EgovLoggingExceptionHandler 다")
	void handlersAreRuntimeLoggingHandler() {
		DefaultListableBeanFactory factory = beanFactory();

		assertEquals(EgovLoggingExceptionHandler.class.getName(), factory.getBeanDefinition("egovHandler").getBeanClassName());
		assertEquals(EgovLoggingExceptionHandler.class.getName(), factory.getBeanDefinition("otherHandler").getBeanClassName());
		assertInstanceOf(ExceptionHandler.class, factory.getBean("egovHandler"));
		assertInstanceOf(ExceptionHandler.class, factory.getBean("otherHandler"));
	}

	@Test
	@DisplayName("핸들러는 예외를 로깅만 하고 다시 던지지 않는다 — null 인자에도 안전")
	void occurNeverThrows() {
		ExceptionHandler handler = beanFactory().getBean("egovHandler", ExceptionHandler.class);

		assertDoesNotThrow(() -> handler.occur(new IllegalStateException("업무 예외"), "egovframework.com.cmm"));
		assertDoesNotThrow(() -> handler.occur(null, null));
	}

	@Test
	@DisplayName("두 핸들러 매니저가 참조하는 핸들러 빈이 정의돼 있다")
	void managersReferenceDefinedHandlers() {
		DefaultListableBeanFactory factory = beanFactory();

		assertEquals(true, factory.containsBeanDefinition("egovHandler"));
		assertEquals(true, factory.containsBeanDefinition("otherHandler"));
	}
}
