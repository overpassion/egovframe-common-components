/**
 *  Class Name : EgovFileScrty.java
 *  Description : Base64인코딩/디코딩 방식을 이용한 데이터를 암호화/복호화하는 Business Interface class
 *  Modification Information
 *
 *     수정일         수정자                   수정내용
 *   -------    --------    ---------------------------
 *   2009.02.04    박지욱          최초 생성
 *
 *  @author 공통 서비스 개발팀 박지욱
 *  @since 2009. 02. 04
 *  @version 1.0
 *  @see
 *
 *  Copyright (C) 2009 by MOPAS  All right reserved.
 */
package egovframework.com.utl.sim.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Base64;

import org.apache.commons.io.FilenameUtils;
import org.egovframe.rte.fdl.filehandling.EgovFiles;

import egovframework.com.cmm.service.EgovProperties;
import org.egovframe.rte.fdl.logging.util.EgovResourceReleaser;

/**
 * @Class Name : EgovFileScrty.java
 * @Description : 파일 및 텍스트 문자열 암호화 처리하는 구현 클래스
 * @Modification Information
 *
 *    수정일                 수정자              수정내용
 *    ----------    -------     -------------------
 *    2019.11.29	신용호		encryptPassword(String data) 삭제 : KISA 보안약점 조치 (비밀번호 해시함수 적용 시 솔트를 사용하여야 함)
 *    2022.11.16	신용호        소스코드 보안 조치
 *
 * @author 공통컴포넌트개발팀 한성곤
 * @since 2009.08.26
 * @version 1.0
 */
public class EgovFileScrty {

	private static final String STORE_FILE_PATH = EgovProperties.getProperty("Globals.fileStorePath");
    // 파일구분자
    static final char FILE_SEPARATOR = File.separatorChar;

    static final int BUFFER_SIZE = 1024;

    /**
     * 파일을 암호화하는 기능
     *
     * @param String source 암호화할 파일
     * @param String target 암호화된 파일
     * @return boolean result 암호화여부 True/False
     * @exception Exception
     */
    public static boolean encryptFile(String source, String target) throws Exception {

		// 암호화 여부
		boolean result = false;

		File srcFile = storedFile(source);

		BufferedInputStream input = null;
		BufferedOutputStream output = null;

		byte[] buffer = new byte[BUFFER_SIZE];

		try {
		    if (srcFile.exists() && srcFile.isFile()) {

				input = new BufferedInputStream(new FileInputStream(srcFile));
				output = new BufferedOutputStream(new FileOutputStream(storedFile(target)));

				int length = 0;
				while ((length = input.read(buffer)) >= 0) {
					byte[] data = new byte[length];
					System.arraycopy(buffer, 0, data, 0, length);
					output.write(encodeBinary(data).getBytes());
					output.write(System.getProperty("line.separator").getBytes());
				}
				result = true;
			}
		} finally {
			EgovResourceReleaser.close(input, output);
		}

		return result;
    }

    /**
     * 파일을 복호화하는 기능
     *
     * @param String source 복호화할 파일
     * @param String target 복호화된 파일
     * @return boolean result 복호화여부 True/False
     * @exception Exception
     */
    public static boolean decryptFile(String source, String target) throws Exception {

		// 복호화 여부
		boolean result = false;

		File srcFile = storedFile(source);

		BufferedReader input = null;
		BufferedOutputStream output = null;

		//byte[] buffer = new byte[BUFFER_SIZE];
		String line = null;

		try {
		    if (srcFile.exists() && srcFile.isFile()) {

			input = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile)));
			output = new BufferedOutputStream(new FileOutputStream(storedFile(target)));

			while ((line = input.readLine()) != null) {
			    byte[] data = line.getBytes();
			    output.write(decodeBinary(new String(data)));
			}

			result = true;
		    }
		} finally {
			EgovResourceReleaser.close(input, output);
		}

		return result;
    }

    /**
     * 데이터를 암호화하는 기능
     *
     * @param byte[] data 암호화할 데이터
     * @return String result 암호화된 데이터
     * @exception Exception
     */
    public static String encodeBinary(byte[] data) throws Exception {
		if (data == null) {
		    return "";
		}

		return new String(Base64.encodeBase64(data));
    }

    /**
     * 데이터를 암호화하는 기능
     *
     * @param String data 암호화할 데이터
     * @return String result 암호화된 데이터
     * @exception Exception
     */
    @Deprecated
    public static String encode(String data) throws Exception {
    	return encodeBinary(data.getBytes());
    }

    /**
     * 데이터를 복호화하는 기능
     *
     * @param String data 복호화할 데이터
     * @return String result 복호화된 데이터
     * @exception Exception
     */
    public static byte[] decodeBinary(String data) throws Exception {
    	return Base64.decodeBase64(data.getBytes());
    }

    /**
     * 데이터를 복호화하는 기능
     *
     * @param String data 복호화할 데이터
     * @return String result 복호화된 데이터
     * @exception Exception
     */
    @Deprecated
    public static String decode(String data) throws Exception {
    	return new String(decodeBinary(data));
    }

    /**
     * 비밀번호를 암호화하는 기능(복호화가 되면 안되므로 SHA-256 인코딩 방식 적용)
     *
     * @param password 암호화될 패스워드
     * @param id salt로 사용될 사용자 ID 지정
     * @return
     * @throws Exception
     */
    public static String encryptPassword(String password, String id) throws Exception {

		if ((password == null) || (id == null))
		 {
			return ""; // KISA 보안약점 조치 (2018-12-11, 신용호)
		}

		byte[] hashValue = null; // 해쉬값

		MessageDigest md = MessageDigest.getInstance("SHA-256");

		md.reset();
		md.update(id.getBytes());

		hashValue = md.digest(password.getBytes());

		return new String(Base64.encodeBase64(hashValue));
    }

    /**
     * 비밀번호를 암호화하는 기능(복호화가 되면 안되므로 SHA-256 인코딩 방식 적용)
     * @param data 암호화할 비밀번호
     * @param salt Salt
     * @return 암호화된 비밀번호
     * @throws Exception
     */
    public static String encryptPassword(String data, byte[] salt) throws Exception {

		if (data == null) {
		    return "";
		}

		byte[] hashValue = null; // 해쉬값

		MessageDigest md = MessageDigest.getInstance("SHA-256");

		md.reset();
		md.update(salt);

		hashValue = md.digest(data.getBytes());

		return new String(Base64.encodeBase64(hashValue));
    }

    /**
     * 비밀번호를 암호화된 패스워드 검증(salt가 사용된 경우만 적용).
     *
     * @param data 원 패스워드
     * @param encoded 해쉬처리된 패스워드(Base64 인코딩)
     * @return
     * @throws Exception
     */
    public static boolean checkPassword(String data, String encoded, byte[] salt) throws Exception {
    	byte[] hashValue = null; // 해쉬값

    	MessageDigest md = MessageDigest.getInstance("SHA-256");

    	md.reset();
    	md.update(salt);
    	hashValue = md.digest(data.getBytes());

    	return MessageDigest.isEqual(hashValue, Base64.decodeBase64(encoded.getBytes()));
    }


	/**
	 * 저장소 기준 디렉토리 안으로 안전하게 해석한 파일을 돌려준다.
	 *
	 * <p>종전에는 {@code STORE_FILE_PATH + FilenameUtils.getName(name)} 로 이어붙이고
	 * {@code EgovWebUtil.filePathBlackList} 로 {@code ".."} 를 지웠다. 두 가지가 잘못돼 있었다.</p>
	 * <ul>
	 *   <li><b>구분자가 빠졌다.</b> {@code Globals.fileStorePath} 는 끝에 구분자가 없어
	 *       {@code /upload/allinone} + {@code REPORT.TXT} 가 {@code /upload/allinoneREPORT.TXT} 가 됐다.</li>
	 *   <li><b>정상 파일명을 훼손했다.</b> {@code ".."} 삭제 때문에 {@code report..2026.pdf} 가
	 *       {@code report2026.pdf} 로 바뀌어 엉뚱한 파일을 읽고 썼다.</li>
	 * </ul>
	 *
	 * <p>경로 이탈은 {@code FilenameUtils.getName} 이 이미 막고 있었고,
	 * {@link EgovFiles#resolveSecurely}가 널 바이트·빈 이름까지 함께 거부한다.</p>
	 *
	 * @param name 파일명(경로가 섞여 있어도 이름만 쓴다)
	 * @return 저장소 안으로 확정된 파일
	 */
	private static File storedFile(String name) {
		return EgovFiles.resolveSecurely(Paths.get(STORE_FILE_PATH), FilenameUtils.getName(name)).toFile();
	}

}