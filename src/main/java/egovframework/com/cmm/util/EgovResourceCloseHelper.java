package egovframework.com.cmm.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Wrapper;

/**
 * Utility class to support to close resources
 * 
 * @author Vincent Han
 * @since 2014.09.18
 * @version 1.0
 * @see
 * 
 *      <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2014.09.18  표준프레임워크센터  최초 생성
 *   2025.05.28  이백행          PMD로 소프트웨어 보안약점 진단하고 제거하기-CloseResource(리소스 닫기)
 *   2026.09.04  실행환경팀        실행환경 EgovResourceReleaser 로 이전, 폐기 예정 표시
 *
 *      </pre>
 *
 * @deprecated 실행환경 {@link org.egovframe.rte.fdl.logging.util.EgovResourceReleaser} 와
 *             메서드 시그니처·동작이 같은 사본이다. 실행환경 것을 쓴다. 공통컴포넌트 안의
 *             호출부는 모두 옮겼다(2026.09 기준 0곳).
 *             <p><b>로그 동작 차이가 하나 있다.</b> 이 클래스는 무시할 예외를
 *             {@code Level.OFF} 로 기록하는데, {@code java.util.logging} 에서
 *             {@code Level.OFF} 는 값이 가장 커서 <b>오히려 항상 출력된다</b> —
 *             "무시" 라는 이름과 정반대다. 실행환경 쪽은 {@code Level.ALL} 이라
 *             기본 설정에서 출력되지 않는다.</p>
 */
@Deprecated
public class EgovResourceCloseHelper {
	/**
	 * Resource close 처리.
	 * 
	 * @param resources
	 */
	public static void close(Closeable... resources) {
		for (Closeable resource : resources) { // NOPMD - CloseResource
			if (resource != null) {
				try {
					resource.close();
				} catch (IOException ignore) {// KISA 보안약점 조치 (2018-10-29, 윤창원)
					EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
				}
			}
		}
	}

	/**
	 * JDBC 관련 resource 객체 close 처리
	 * 
	 * @param objects
	 */
	public static void closeDBObjects(Wrapper... objects) {
		for (Object object : objects) {
			if (object != null) {
				if (object instanceof ResultSet) {
					try {
						((ResultSet) object).close();
					} catch (SQLException ignore) {// KISA 보안약점 조치 (2018-10-29, 윤창원)
						EgovBasicLogger.ignore("Occurred SQLException to close resource is ingored!!");
					}
				} else if (object instanceof Statement) {
					try {
						((Statement) object).close();
					} catch (SQLException ignore) {// KISA 보안약점 조치 (2018-10-29, 윤창원)
						EgovBasicLogger.ignore("Occurred SQLException to close resource is ingored!!");
					}
				} else if (object instanceof Connection) {
					try {
						((Connection) object).close();
					} catch (SQLException ignore) {
						EgovBasicLogger.ignore("Occurred SQLException to close resource is ingored!!");
					}
				} else {
					throw new IllegalArgumentException("Wrapper type is not found : " + object.toString());
				}
			}
		}
	}

	/**
	 * Socket 관련 resource 객체 close 처리
	 * 
	 * @param objects
	 */
	public static void closeSocketObjects(Socket socket, ServerSocket server) {
		if (socket != null) {
			try {
				socket.shutdownOutput();
			} catch (IOException ignore) {
				EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
			}

			try {
				socket.close();
			} catch (IOException ignore) {
				EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
			}
		}

		if (server != null) {
			try {
				server.close();
			} catch (IOException ignore) {
				EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
			}
		}
	}

	/**
	 * Socket 관련 resource 객체 close 처리
	 * 
	 * @param sockets
	 */
	public static void closeSockets(Socket... sockets) {
		for (Socket socket : sockets) { // NOPMD - CloseResource
			if (socket != null) {
				try {
					socket.shutdownOutput();
				} catch (IOException ignore) {
					EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
				}

				try {
					socket.close();
				} catch (IOException ignore) {
					EgovBasicLogger.ignore("Occurred IOException to close resource is ingored!!");
				}
			}
		}
	}
}