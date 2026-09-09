package egovframework.com.cmm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * egovc.tld 의 {@code sanitizeHtml} EL 함수가 실행환경 ptl.mvc 의 EgovHtmlSanitizer 로 배선된 것을 고정한다.
 *
 * <p>TLD 를 파싱해 function-class·function-signature 를 읽고, 그 클래스의 메서드가 실제로 존재하며 스크립트를
 * 제거하는지 확인한다. 조회 화면 6곳({@code egovc:sanitizeHtml(...)})이 이 함수를 쓴다.</p>
 */
class EgovcTldSanitizeFunctionTest {

	private static final File TLD = new File("src/main/webapp/WEB-INF/tlds/egovc.tld");

	@Test
	@DisplayName("sanitizeHtml 함수는 실행환경 EgovHtmlSanitizer.sanitize(String) 을 가리킨다")
	void functionClassIsRuntimeSanitizer() throws Exception {
		Element function = sanitizeFunction();

		assertEquals("org.egovframe.rte.ptl.mvc.filter.EgovHtmlSanitizer", text(function, "function-class"));
		assertEquals("java.lang.String sanitize(java.lang.String)", text(function, "function-signature"));
	}

	@Test
	@DisplayName("선언된 메서드가 실제로 존재하고 스크립트를 제거한다")
	void declaredMethodExistsAndSanitizes() throws Exception {
		Element function = sanitizeFunction();
		Class<?> type = Class.forName(text(function, "function-class"));
		Method sanitize = type.getMethod("sanitize", String.class);

		String out = (String) sanitize.invoke(null, "<p>본문</p><script>alert(1)</script><img src=x onerror=alert(1)>");

		assertTrue(out.contains("<p>본문</p>"), out);
		assertFalse(out.contains("<script"), out);
		assertFalse(out.contains("onerror"), out);
	}

	private static Element sanitizeFunction() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Document doc = factory.newDocumentBuilder().parse(TLD);
		NodeList functions = doc.getElementsByTagName("function");
		for (int i = 0; i < functions.getLength(); i++) {
			Element function = (Element) functions.item(i);
			if ("sanitizeHtml".equals(text(function, "name"))) {
				return function;
			}
		}
		assertNotNull(null, "egovc.tld 에 sanitizeHtml 함수가 없다");
		return null;
	}

	private static String text(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent().trim();
	}
}
