package egovframework.com.cmm;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.egovframe.rte.fdl.filehandling.EgovContentDispositions;

/**
 * 웹브라우저 종류및 버전 파악하기 ( IE및 Edge, Safari, Chrome, Firefox, Opera )
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일              수정자              수정내용
 *  -----------  --------    ---------------------------
 *   2018.08.27  신용호              최초 생성
 *   2026.07.11  이석곤              브라우저 판별 정규식을 static final로 호이스팅 (호출마다 재컴파일 제거)
 *   2026.09.10  실행환경팀            getDisposition 을 실행환경 EgovContentDispositions(RFC 6266) 위임으로 교체 — 브라우저 판별 분기·미지원 브라우저 예외 제거
 *
 * </pre>
 */

public class EgovBrowserUtil {
	
	public static final String FIREFOX = "Firefox";
	public static final String SAFARI = "Safari";
	public static final String CHROME = "Chrome";
	public static final String OPERA = "Opera";
	public static final String MSIE = "MSIE";
	public static final String EDGE = "Edge";
	public static final String WHALE = "Whale";
	public static final String OTHER = "Other";
	
	public static final String TYPEKEY = "type";
	public static final String VERSIONKEY = "version";

	// 브라우저 판별 정규식 — 호출마다 재컴파일하지 않도록 클래스 로딩 시 1회만 컴파일한다.
	private static final Pattern MSIE_PATTERN = Pattern.compile("MSIE ([0-9]{1,2}.[0-9])");
	private static final Pattern EDGE_PATTERN = Pattern.compile("Edge/([0-9]{1,3}.[0-9]{1,5})");
	private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/([0-9]{1,3}.[0-9]{1,3})");
	private static final Pattern OPERA_PATTERN = Pattern.compile("OPR/([0-9]{1,3}.[0-9]{1,3})");
	private static final Pattern WHALE_PATTERN = Pattern.compile("Whale/([0-9]{1,3}\\.[0-9]{1,3})");
	private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/([0-9]{1,3}.[0-9]{1,3})");
	private static final Pattern SAFARI_PATTERN = Pattern.compile("Version/([0-9]{1,2}.[0-9]{1,3})");

	public static HashMap<String,String> getBrowser(String userAgent) {
		
		HashMap<String,String> result = new HashMap<String,String>();
		Matcher matcher = null;

		matcher = MSIE_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,MSIE);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;
		}
		
		if (userAgent.contains("Trident/7.0")) {
		    result.put(TYPEKEY,MSIE);
		    result.put(VERSIONKEY,"11.0");
		    return result;
		}
		
		matcher = EDGE_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,EDGE);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;
		}
		
		matcher = FIREFOX_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,FIREFOX);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;		    
		}

		matcher = OPERA_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,OPERA);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;		    
		}

		matcher = WHALE_PATTERN.matcher(userAgent);
		if (matcher.find()) {
			result.put(TYPEKEY, WHALE);
			result.put(VERSIONKEY, matcher.group(1));
			return result;
		}

		matcher = CHROME_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,CHROME);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;		    
		}
		
		matcher = SAFARI_PATTERN.matcher(userAgent);
		if (matcher.find())
		{
		    result.put(TYPEKEY,SAFARI);
		    result.put(VERSIONKEY,matcher.group(1));
			return result;		    
		}

	    result.put(TYPEKEY,OTHER);
	    result.put(VERSIONKEY,"0.0");
		return result;
	}
	
	/**
	 * 첨부 다운로드용 Content-Disposition 헤더값을 만든다.
	 *
	 * <p>종전에는 브라우저 종류·버전으로 헤더 형식을 갈랐다 — IE 8 이하는 값 앞에 헤더 이름까지 붙인 잘못된 형식이었고,
	 * 판별되지 않는 브라우저는 {@code RuntimeException("Not supported browser")} 로 다운로드 자체가 막혔으며,
	 * 그 외는 ASCII 폴백 없는 {@code filename*} 만 실었다. 지금은 실행환경 {@link EgovContentDispositions} 의
	 * RFC 6266 표준 형식 하나로 통일한다(비ASCII 는 {@code filename*=UTF-8''} + ASCII 폴백 병기, 제어문자 제거).</p>
	 *
	 * @param filename 원본 파일명
	 * @param userAgent 무시된다(호환용)
	 * @param charSet 무시된다(항상 UTF-8)
	 * @return Content-Disposition 헤더값
	 * @deprecated 실행환경 {@link EgovContentDispositions#attachment(String)} 를 직접 사용한다.
	 */
	@Deprecated
	public static String getDisposition(String filename, String userAgent, String charSet) {
		return EgovContentDispositions.attachment(filename);
	}

	//KISA 보안약점 조치 (2018-10-29, 윤창원)
/*	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String testUserAgent[] = {
		// IE 7.0
		"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)"
		,"Mozilla/4.0 (Mozilla/4.0; MSIE 7.0; Windows NT 5.1; FDM; SV1; .NET CLR 3.0.04506.30)"
		// IE 8.0
		,"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)"
		// IE 9.0
		,"Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)"
		// IE 10.0
		,"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)"
		// IE 11.0
		,"Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko"
		// Chrome 68.0.3440.106
		,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36"
		// Edge 17.17134
		,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134"
		// Opera 55.0.2994.44
		,"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36 OPR/55.0.2994.44"
		,"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36 OPR/26.0.1656.60"
		// Firefox 61.0
		,"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:61.0) Gecko/20100101 Firefox/61.0"
		// Safari 11.1.2
		,"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.2 Safari/605.1.15"
		// iPhone 11.0
		,"Mozilla/5.0 (iPhone; CPU iPhone OS 11_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.0 Mobile/15E148 Safari/604.1"
		// iPad 9.0
		,"Mozilla/5.0 (iPad; CPU OS 9_3_5 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G36 Safari/601.1"
		// Window Pohone 10
		,"Mozilla/5.0 (Windows Phone 10.0;  Android 4.2.1; Nokia; Lumia 520) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Mobile Safari/537.36 Edge/12.0"
		// Window Pohone 8.1
		,"Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 520) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537"
		// Window Pohone 8
		,"Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch)"
		// Window Pohone 7
		,"Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0)"
		// XBOX One
		,"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0; Xbox; Xbox One)"
		// XBOX 360
		,"Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; Xbox)"
		};
		
		HashMap<String,String> result = null;
		for (int i = 0; i < testUserAgent.length; i++) {
			result = getBrowser(testUserAgent[i]);
		}

	}*/

}
