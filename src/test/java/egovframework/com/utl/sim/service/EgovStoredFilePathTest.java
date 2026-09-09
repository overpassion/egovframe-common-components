package egovframework.com.utl.sim.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import egovframework.com.cmm.service.EgovProperties;

/**
 * 저장소 파일 경로 조립 검증 — {@code EgovWebUtil.filePathBlackList} 이어붙이기를
 * 실행환경 {@code EgovFiles.resolveSecurely} 로 바꾼 결과를 고정한다.
 *
 * <p>종전 방식에는 결함이 둘 있었다.</p>
 * <ul>
 *   <li><b>구분자 누락</b> — {@code Globals.fileStorePath} 는 끝에 구분자가 없다
 *       ({@code /upload/allinone}). 여기에 파일명을 그냥 붙여
 *       {@code /upload/allinoneREPORT.TXT} 가 됐다. {@code EgovPdfCnvr} 은 쓸 때만 구분자를
 *       넣어 <b>자기가 쓴 파일을 다시 읽지 못했다.</b></li>
 *   <li><b>정상 파일명 훼손</b> — {@code ".."} 를 지우는 방식이라
 *       {@code report..2026.pdf} 가 {@code report2026.pdf} 가 됐다.</li>
 * </ul>
 *
 * <p>두 클래스의 파일 메서드는 호출부가 없어(암호는 {@code encryptPassword} 계열을 쓴다)
 * 이 결함이 운영에 드러난 적은 없다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovFiles 경로 조립 이전 검증)
 * </pre>
 */
class EgovStoredFilePathTest {

	private static final String BS = String.valueOf((char) 92);

	private static File storedFile(Class<?> owner, String method, String name) throws Exception {
		Method m = owner.getDeclaredMethod(method, String.class);
		m.setAccessible(true);
		return (File) m.invoke(null, name);
	}

	private static File scrty(String name) throws Exception {
		return storedFile(EgovFileScrty.class, "storedFile", name);
	}

	private static File pdf(String name) throws Exception {
		return storedFile(EgovPdfCnvr.class, "storedFile", name);
	}

	private static File expected(String name) {
		return Paths.get(EgovProperties.getProperty("Globals.fileStorePath"))
				.toAbsolutePath().normalize().resolve(name).toFile();
	}

	@Test
	@DisplayName("기준 디렉터리와 파일명 사이에 구분자가 들어간다")
	void 구분자가_들어간다() throws Exception {
		File file = scrty("REPORT.TXT");

		assertEquals(expected("REPORT.TXT"), file);
		assertEquals("REPORT.TXT", file.getName());
		assertTrue(file.getParent().endsWith("allinone"),
				"종전에는 allinoneREPORT.TXT 로 붙어 상위 폴더에 떨어졌다: " + file);
	}

	@Test
	@DisplayName("두 클래스가 같은 위치를 가리킨다 — 종전에는 쓰기와 읽기가 어긋났다")
	void 두_클래스가_같은_위치() throws Exception {
		assertEquals(scrty("A.TXT"), pdf("A.TXT"));
	}

	@Test
	@DisplayName("점 두 개가 든 정상 파일명이 훼손되지 않는다")
	void 정상_파일명_보존() throws Exception {
		File file = scrty("report..2026.pdf");

		assertEquals("report..2026.pdf", file.getName(),
				"종전에는 report2026.pdf 로 바뀌어 엉뚱한 파일을 읽고 썼다");
	}

	@Test
	@DisplayName("경로가 섞여 와도 이름만 쓴다")
	void 경로는_떼어낸다() throws Exception {
		assertEquals(expected("passwd"), scrty("../../etc/passwd"));
		assertEquals(expected("a.txt"), scrty("dir" + BS + "a.txt"));
	}

	@Test
	@DisplayName("빈 이름과 널 바이트는 거부한다")
	void 잘못된_이름_거부() {
		for (String bad : new String[] { "", "   ", "a.txt" + (char) 0 }) {
			InvocationTargetException e = assertThrows(InvocationTargetException.class,
					() -> scrty(bad), "거부되어야 한다: [" + bad + "]");
			assertTrue(e.getCause() instanceof IllegalArgumentException,
					"원인: " + e.getCause());
		}
	}
}
