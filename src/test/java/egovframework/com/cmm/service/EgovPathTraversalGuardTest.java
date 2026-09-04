package egovframework.com.cmm.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.egovframe.rte.fdl.filehandling.EgovFiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * 경로 조작 방어 계약 검증 — 자체 구현을 실행환경 {@link EgovFiles} 로 넘긴 결과를 고정한다.
 *
 * <p>두 곳이 이 계약에 기대고 있다.</p>
 * <ul>
 *   <li>{@code FileSystemUtils} 배치 실행 경로 — 화이트리스트 뒤의 최후 방어선</li>
 *   <li>{@code EgovWebEditorImageController.download} 이미지 조회 경로</li>
 * </ul>
 *
 * <p><b>실측으로 확인한 차이 두 가지.</b> 종전 구현은 ① {@code ".."} 문자열 검사와
 * ② 정규화 후 {@code startsWith} 검사를 함께 썼다.</p>
 * <ul>
 *   <li><b>널 바이트</b> — 종전에는 {@code Path.resolve} 가 {@link InvalidPathException} 을 던졌고
 *       그것을 잡는 곳이 없어 그대로 밖으로 나갔다. 새 구현은 조용히 거부로 판정한다.</li>
 *   <li><b>{@code a/../b.png} 처럼 탈출하지 않는 {@code ..}</b> — 종전의 문자열 검사가
 *       무해한 경로까지 막았다. 새 구현은 <b>정규화 결과가 기준 안</b>이면 허용한다.
 *       실제로 탈출하는 경로는 양쪽 모두 막으므로 방어가 약해진 것은 아니다.
 *       화면이 넘기는 값은 날짜 폴더와 32자리 16진 파일명이라 {@code ..} 가 들어갈 일이 없다.</li>
 * </ul>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovFiles 경로 봉쇄 이전 검증)
 * </pre>
 */
class EgovPathTraversalGuardTest {

	private static final String BS = String.valueOf((char) 92);

	private static final Path BASE = Paths.get("target", "path-guard-base");

	/** 종전 구현의 판정을 그대로 재현한다 — 예외를 잡지 않는 것까지 같다. */
	private static boolean legacyRejects(String relative) {
		if (relative.contains("..")) {
			return true;
		}
		Path base = BASE.toAbsolutePath().normalize();
		return !base.resolve(relative).normalize().startsWith(base);
	}

	private static boolean rejects(String relative) {
		return EgovFiles.tryResolveSecurely(BASE, relative).isEmpty();
	}

	/** 정규화 결과가 기준 밖으로 나가는가 — 실제 탈출 여부. */
	private static boolean actuallyEscapes(String relative) {
		Path base = BASE.toAbsolutePath().normalize();
		return !base.resolve(relative).normalize().startsWith(base);
	}

	@Test
	@DisplayName("정상 상대 경로는 통과한다")
	void 정상_경로() {
		for (String ok : new String[] { "20260904/ABCDEF.png", "a.png", "sub/dir/file.jpg" }) {
			assertFalse(rejects(ok), ok);
			assertFalse(legacyRejects(ok), "종전에도 통과: " + ok);
		}
	}

	@Test
	@DisplayName("실제로 탈출하는 경로는 모두 막는다 — 방어가 약해진 지점이 없다")
	void 실제_탈출은_전부_차단() {
		String[] inputs = { "a.png", "sub/a.png", "../a.png", "a/../b.png", "a/../../b.png", "..",
			"./a.png", "sub/./a.png", "../../etc/passwd" };

		int escaped = 0;
		for (String in : inputs) {
			if (actuallyEscapes(in) && !rejects(in)) {
				escaped++;
				System.out.println("탈출 허용됨: " + in);
			}
		}

		assertEquals(0, escaped, "기준 디렉토리를 벗어나는 입력이 통과하면 안 된다");
	}

	@Test
	@DisplayName("상위 경로 탈출은 막는다 — 종전과 같다")
	void 상위_탈출() {
		for (String bad : new String[] { "../etc/passwd", "a/../../b", ".." }) {
			assertTrue(rejects(bad), bad);
			assertTrue(legacyRejects(bad), "종전에도 차단: " + bad);
		}
	}

	@Test
	@DisplayName("탈출하지 않는 \"..\" 는 이제 통과한다 — 종전 문자열 검사가 과했다")
	void 무해한_상위표기() {
		String harmless = "a/../b.png";

		assertFalse(actuallyEscapes(harmless), "정규화하면 기준 안이다");
		assertTrue(legacyRejects(harmless), "종전에는 문자열 검사에 걸려 막혔다");
		assertFalse(rejects(harmless), "새 구현은 실제 위치로 판정해 통과시킨다");
	}

	@Test
	@DisplayName("널 바이트는 조용히 거부한다 — 종전에는 InvalidPathException 이 그대로 나갔다")
	void 널바이트() {
		String attack = "a.png" + (char) 0 + ".jsp";

		assertTrue(rejects(attack), "새 구현은 거부로 판정한다");
		assertThrows(InvalidPathException.class, () -> legacyRejects(attack),
				"종전에는 예외가 밖으로 나가 500 으로 이어졌다");
	}

	@Test
	@DisplayName("빈 값을 막는다")
	void 빈_값() {
		assertTrue(rejects(""));
	}

	@Test
	@DisplayName("절대 경로를 막는다")
	@EnabledOnOs(OS.WINDOWS)
	void 절대경로_윈도우() {
		assertTrue(rejects("C:" + BS + "Windows" + BS + "system32" + BS + "cmd.exe"));
	}

	@Test
	@DisplayName("절대 경로를 막는다")
	@EnabledOnOs({ OS.LINUX, OS.MAC })
	void 절대경로_유닉스() {
		assertTrue(rejects("/etc/passwd"));
	}
}
