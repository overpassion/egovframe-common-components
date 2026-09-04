package egovframework.com.utl.fcc.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;

import org.egovframe.rte.fdl.string.EgovLunarDate;
import org.egovframe.rte.fdl.string.EgovLunarDates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 음력 변환 이전 검증 — ICU {@code ChineseCalendar}(중국 역법)에서 실행환경
 * {@link EgovLunarDates}(단기력)로 옮긴 결과를 고정한다.
 *
 * <p><b>왜 옮겼나.</b> 한국 공공기관이 쓰는 음력은 단기력이다. 두 역법은 삭(朔)을
 * 계산하는 기준 시간대가 달라 일부 달의 경계가 하루 어긋난다 — 2000~2026 표본
 * 1,620건 중 60건(3.7%)이 다르고, 윤달 판정이 갈리는 해는 한 달까지 벌어졌다.</p>
 *
 * <p><b>무엇을 유지했나.</b> 음력 달은 29일 또는 30일이라 "음력 2월 30일" 이 없는 해가
 * 있다. 매년 반복 기념일이 저장된 월·일을 올해에 다시 붙이는 방식이라 이런 값이 실제로
 * 생긴다(2020~2030 표본의 1.5%). 종전 ICU 는 관대 모드로 조용히 다음 달로 넘겼고,
 * 그 동작을 그대로 유지한다 — 예외를 던지면 기념일 화면이 500 으로 죽는다.</p>
 *
 * <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2026.09.04  실행환경팀        최초 생성 (EgovLunarDates 이전 검증)
 * </pre>
 */
class EgovDateUtilLunarTest {

	private static String solarOf(int year, int month, int day) {
		LocalDate d = EgovLunarDates.toSolar(EgovLunarDate.of(year, month, day));
		return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
	}

	@Test
	@DisplayName("존재하는 음력 날짜는 실행환경 API 와 완전히 같다")
	void 실행환경과_동일() {
		int checked = 0;
		for (int y = 2000; y <= 2030; y++) {
			for (int m = 1; m <= 12; m++) {
				for (int d = 1; d <= 30; d++) {
					String expected;
					try {
						expected = solarOf(y, m, d);
					} catch (IllegalArgumentException notInCalendar) {
						continue;
					}
					assertEquals(expected, EgovDateUtil.toSolar(String.format("%04d%02d%02d", y, m, d), 0),
							String.format("음력 %04d%02d%02d", y, m, d));
					checked++;
				}
			}
		}
		assertEquals(10990, checked, "표본 수가 달라지면 달력 데이터가 바뀐 것이다");
	}

	@Test
	@DisplayName("없는 음력 날짜는 예외 없이 다음 달로 넘긴다 — 기념일 화면이 죽지 않도록")
	void 없는_날짜는_넘긴다() {
		assertEquals("20200324", assertDoesNotThrow(() -> EgovDateUtil.toSolar("20200230", 0)));
		assertEquals("20250329", assertDoesNotThrow(() -> EgovDateUtil.toSolar("20250230", 0)));

		// 넘긴 결과는 다음 달 1일과 같다
		assertEquals(solarOf(2020, 3, 1), EgovDateUtil.toSolar("20200230", 0));
	}

	@Test
	@DisplayName("양력 -> 음력 -> 양력 왕복이 어긋나지 않는다")
	void 왕복_변환() {
		LocalDate day = LocalDate.of(2020, 1, 1);
		while (day.isBefore(LocalDate.of(2027, 1, 1))) {
			String solar = String.format("%04d%02d%02d", day.getYear(), day.getMonthValue(), day.getDayOfMonth());

			Map<String, String> lunar = EgovDateUtil.toLunar(solar);
			String back = EgovDateUtil.toSolar(lunar.get("day"), Integer.parseInt(lunar.get("leap")));

			assertEquals(solar, back, "왕복 실패: 음력 " + lunar.get("day") + " 윤" + lunar.get("leap"));
			day = day.plusDays(1);
		}
	}

	@Test
	@DisplayName("설날·추석은 종전과 같다 — 실제 쓰임에서 달라지지 않는다")
	void 명절은_그대로() {
		assertEquals("20240210", EgovDateUtil.toSolar("20240101", 0));
		assertEquals("20250129", EgovDateUtil.toSolar("20250101", 0));
		assertEquals("20260217", EgovDateUtil.toSolar("20260101", 0));

		assertEquals("20240917", EgovDateUtil.toSolar("20240815", 0));
		assertEquals("20251006", EgovDateUtil.toSolar("20250815", 0));
		assertEquals("20260925", EgovDateUtil.toSolar("20260815", 0));
	}

	@Test
	@DisplayName("윤달을 가려낸다")
	void 윤달() {
		// 2020년은 윤4월이 있다
		assertEquals("20200507", EgovDateUtil.toSolar("20200415", 0));
		assertEquals("20200606", EgovDateUtil.toSolar("20200415", 1));

		Map<String, String> lunar = EgovDateUtil.toLunar("20200523");
		assertEquals("20200401", lunar.get("day"));
		assertEquals("1", lunar.get("leap"), "양력 2020-05-23 은 윤4월 1일이다");
	}

	@Test
	@DisplayName("윤달이 없는 달에 윤달을 요청하면 평달로 본다")
	void 윤달이_없으면_평달() {
		assertEquals(EgovDateUtil.toSolar("20240510", 0), EgovDateUtil.toSolar("20240510", 1));
	}

	@Test
	@DisplayName("월이 범위를 벗어나면 예외를 던진다 — 종전에는 조용히 다음 해로 넘겼다")
	void 잘못된_월() {
		assertThrows(IllegalArgumentException.class, () -> EgovDateUtil.toSolar("20241310", 0));
		assertThrows(IllegalArgumentException.class, () -> EgovDateUtil.toSolar("20240010", 0));
	}

	@Test
	@DisplayName("입력 형식은 종전 계약을 지킨다")
	void 입력_형식() {
		assertEquals(EgovDateUtil.toSolar("20240101", 0), EgovDateUtil.toSolar("2024-01-01", 0));
		assertThrows(IllegalArgumentException.class, () -> EgovDateUtil.toSolar("2024011", 0));

		Map<String, String> empty = EgovDateUtil.toLunar("2024-01-01");
		assertEquals("20231120", empty.get("day"));
		assertEquals("0", empty.get("leap"));
	}
}
