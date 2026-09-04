package egovframework.com.cmm.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.egovframe.rte.fdl.logging.util.EgovResourceReleaser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 자원 해제 유틸 이전 검증 — 공통컴포넌트 사본 {@code EgovResourceCloseHelper} 를
 * 실행환경 {@link EgovResourceReleaser} 로 바꾼 결과를 고정한다.
 *
 * <p>두 클래스는 메서드 시그니처가 같고 구현도 같다. 차이는 <b>무시한 예외를 어느 레벨로
 * 기록하느냐</b> 하나뿐이며, 그 차이가 눈에 보이는 결과를 바꾼다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovResourceReleaser 이전 검증)
 * </pre>
 */
class EgovResourceReleaserAdoptionTest {

	/** close 가 불렸는지, 예외를 던지는지 기록하는 자원. */
	private static final class Probe implements Closeable {
		private final boolean fail;
		private boolean closed;

		private Probe(boolean fail) {
			this.fail = fail;
		}

		@Override
		public void close() throws IOException {
			closed = true;
			if (fail) {
				throw new IOException("의도적 실패");
			}
		}
	}

	@Test
	@DisplayName("두 유틸의 공개 메서드 시그니처가 같다")
	void 시그니처_동일() {
		List<String> legacy = signatures(EgovResourceCloseHelper.class);
		List<String> modern = signatures(EgovResourceReleaser.class);

		assertEquals(legacy, modern, "이전이 성립하려면 계약이 같아야 한다");
	}

	private static List<String> signatures(Class<?> type) {
		List<String> out = new ArrayList<>();
		for (Method m : type.getDeclaredMethods()) {
			if (!java.lang.reflect.Modifier.isPublic(m.getModifiers())) {
				continue;
			}
			out.add(m.getReturnType().getSimpleName() + " " + m.getName()
					+ Arrays.toString(m.getParameterTypes()));
		}
		java.util.Collections.sort(out);
		return out;
	}

	@Test
	@DisplayName("여러 자원을 모두 닫는다 — 앞에서 예외가 나도 뒤를 건너뛰지 않는다")
	void 전부_닫는다() {
		Probe first = new Probe(true);
		Probe second = new Probe(false);

		EgovResourceReleaser.close(first, second);

		assertTrue(first.closed);
		assertTrue(second.closed, "앞 자원이 예외를 던져도 뒤 자원을 닫아야 한다");
	}

	@Test
	@DisplayName("null 자원은 건너뛴다")
	void null_은_건너뛴다() {
		Probe probe = new Probe(false);

		EgovResourceReleaser.close(null, probe, null);

		assertTrue(probe.closed);
	}

	@Test
	@DisplayName("닫기 예외를 밖으로 내보내지 않는다")
	void 예외를_삼킨다() {
		Probe probe = new Probe(true);

		EgovResourceReleaser.close(probe);

		assertTrue(probe.closed);
	}

	@Test
	@DisplayName("지원하지 않는 DB 객체는 예외를 던진다 — 종전과 같다")
	void 알_수_없는_DB객체() {
		java.sql.Wrapper odd = new java.sql.Wrapper() {
			@Override
			public <T> T unwrap(Class<T> iface) {
				return null;
			}

			@Override
			public boolean isWrapperFor(Class<?> iface) {
				return false;
			}
		};

		assertThrows(IllegalArgumentException.class, () -> EgovResourceReleaser.closeDBObjects(odd));
	}

	@Test
	@DisplayName("무시 메시지가 기본 설정에서 출력되지 않는다 — 종전 Level.OFF 는 오히려 항상 출력됐다")
	void 무시_메시지는_남지_않는다() {
		Logger ignoreLogger = Logger.getLogger("ignore");

		assertFalse(ignoreLogger.isLoggable(Level.ALL), "실행환경 방식(Level.ALL)은 출력되지 않는다");
		assertTrue(ignoreLogger.isLoggable(Level.OFF),
				"종전 방식(Level.OFF)은 값이 가장 커서 오히려 항상 출력됐다 — 이름과 정반대");
	}

	@Test
	@DisplayName("스트림을 닫아도 이미 읽은 내용은 그대로다 — 실사용 형태 확인")
	void 실사용_형태() throws Exception {
		byte[] data = { 1, 2, 3 };
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		byte[] read = in.readAllBytes();

		EgovResourceReleaser.close(in);

		assertArrayEquals(data, read);
	}
}
