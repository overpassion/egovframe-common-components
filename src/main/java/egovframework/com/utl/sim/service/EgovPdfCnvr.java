/**
 *  Class Name : EgovPdfCnvr.java
 *  Description : xls,doc,ppt를 Pdf로 변환하는 화면 Business Interface class
 *  Modification Information
 *
 *     수정일         수정자                   수정내용
 *   -------    --------    ---------------------------
 *   2009.02.02    이 용          최초 생성
 *   2017.03.03          조성원 	    시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
 *
 *  @author 공통 서비스 개발팀 이 용
 *  @since 2009. 02. 02
 *  @version 1.0
 *  @see
 * The type com.sun.star.lang.XeventListener cannot be resolved. It is indirectly referenced from required .class files
 *  Copyright (C) 2009 by EGOV  All right reserved.
 */

package egovframework.com.utl.sim.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.io.FilenameUtils;
import org.egovframe.rte.fdl.filehandling.EgovFiles;
import org.egovframe.rte.fdl.filehandling.upload.EgovStoredFileNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.cmm.util.EgovBasicLogger;
import org.egovframe.rte.fdl.logging.util.EgovResourceReleaser;
import egovframework.com.utl.fcc.service.EgovStringUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class EgovPdfCnvr {
	public static String addrIP = "";
	static final char FILE_SEPARATOR = File.separatorChar;
	// 최대 문자길이
	static final int MAX_STR_LEN = 1024;
	public static final int BUFF_SIZE = 2048;
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovPdfCnvr.class);
	private static final String STORE_FILE_PATH = EgovProperties.getProperty("Globals.fileStorePath");

	/**
	 * <pre>
	 * Comment : doc, xls 파일등을 PDF변환 변환한다.
	 * </pre>
	 * @param String pdfFileSrc        doc, xls 파일 전체경로
	 * @param String targetPdf         변환파일명(확장자 제외)
	 * @return boolean  status         true/false 를 리턴한다.
	 * @version 1.0 (2009.02.10)
	 * @see
	 */
	public static boolean getPDF(String targetPdf, HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean status = false;

		try {
			MultipartHttpServletRequest mptRequest = WebUtils.getNativeRequest(request,MultipartHttpServletRequest.class);

			// 2022.01 Possible null pointer dereference due to return value of called method 조치
			if(mptRequest!= null) {
				Iterator<String> file_iter = mptRequest.getFileNames();

				while (file_iter.hasNext()) {

					MultipartFile mFile = mptRequest.getFile(file_iter.next());

					// 2022.11.11 김혜준 시큐어코딩 처리
					if (mFile == null) {
						continue;
					}

					String newName = "";

					// 변환 원본의 저장명 — 실행환경 EgovStoredFileNames(UUID 32자). 종전 시각 문자열은 같은 밀리초에 충돌했다.
					newName = EgovStoredFileNames.generateWithoutExtension();
					writeFile(mFile, newName);

					File inputFile = storedFile(newName);

					if (inputFile.exists()) {

						// connect to an OpenOffice.org instance running on port 8100
						SocketOpenOfficeConnection connection = new SocketOpenOfficeConnection(8100);
						connection.connect();
						//원본 디렉토리에 targetPdf 명칭지정
						String valueFile = null;
						//KISA 보안약점 조치 (2018-10-29, 윤창원)
						valueFile = EgovStringUtil.isNullToString(inputFile.getParent()).replace('\\', FILE_SEPARATOR).replace('/', FILE_SEPARATOR);
						File outputFile = new File(valueFile + "/" + targetPdf + ".pdf");
						// convert
						DocumentConverter converter = new OpenOfficeDocumentConverter(connection);
						converter.convert(inputFile, outputFile);
						// close the connection
						connection.disconnect();

						if (inputFile.exists()) {
							//3. 삭제해줍니다.
							status = inputFile.delete();
						}

						status = true;

					} else {
						status = false;
					}

				}

			}
		} catch (IOException ex) {
			EgovBasicLogger.debug("PDF converting error", ex);
			status = false;
		}

		// 메소드 종료 Log
		return status;
	}

	/**
	 * 파일을 실제 물리적인 경로에 생성한다.
	 * @param file
	 * @param newName
	 * @param stordFilePath
	 * @throws Exception
	 */
	protected static void writeFile(MultipartFile file, String newName) throws IOException {
		//2026.02.28 KISA 취약점 조치
		if (file == null || file.isEmpty()) {
			throw new IOException("업로드 파일이 없습니다.");
		}
		InputStream stream = null;
		OutputStream bos = null;

		try {

			stream = file.getInputStream();
			//2026.02.28 KISA 취약점 조치
			if (stream == null) {
				throw new IOException("업로드 파일 스트림을 열 수 없습니다.");
			}
	
			File cFile = Paths.get(STORE_FILE_PATH).toFile();

			if (!cFile.isDirectory()) {
				// 2017.03.03 조성원 시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
				if (cFile.mkdirs()) {
					LOGGER.debug("[file.mkdirs] targetDir : Directory Creation Success");
				} else {
					LOGGER.error("[file.mkdirs] targetDir : Directory Creation Fail");
				}
			}

			bos = new FileOutputStream(storedFile(newName));

			int bytesRead = 0;
			byte[] buffer = new byte[BUFF_SIZE];
			while ((bytesRead = stream.read(buffer, 0, BUFF_SIZE)) != -1) {
				bos.write(buffer, 0, bytesRead);
			}

		} finally {
			EgovResourceReleaser.close(bos, stream);
		}
	}

	/**
	 * 저장소 기준 디렉터리 안으로 안전하게 해석한 파일을 돌려준다.
	 *
	 * <p>종전에는 같은 클래스 안에서 이어붙이는 방식이 갈렸다 — 쓸 때는
	 * {@code STORE_FILE_PATH + File.separator + 이름}, 읽을 때는 구분자 없이
	 * {@code STORE_FILE_PATH + 이름} 이었다. {@code Globals.fileStorePath} 끝에 구분자가 없어
	 * 읽기 경로가 {@code /upload/allinone이름} 이 되므로 <b>쓴 파일을 다시 읽지 못했다.</b>
	 * {@link EgovFiles#resolveSecurely}는 구분자를 알아서 맞추고, 널 바이트·빈 이름도 거부한다.</p>
	 *
	 * @param name 파일명(경로가 섞여 있어도 이름만 쓴다)
	 * @return 저장소 안으로 확정된 파일
	 */
	private static File storedFile(String name) {
		return EgovFiles.resolveSecurely(Paths.get(STORE_FILE_PATH), FilenameUtils.getName(name)).toFile();
	}

}