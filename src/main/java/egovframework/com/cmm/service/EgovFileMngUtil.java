package egovframework.com.cmm.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.egovframe.rte.fdl.filehandling.EgovContentDispositions;
import org.egovframe.rte.fdl.filehandling.upload.EgovStoredFileNames;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import egovframework.com.cmm.EgovWebUtil;

/**
 * 파일 관리 유틸리티
 * 
 * @author 공통 서비스 개발팀 이삼섭
 * @since 2009. 02. 13
 * @version 1.0
 * @see
 * 
 *      <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.02.13  이삼섭          최초 생성
 *   2011.08.09  서준식          utl.fcc패키지와 Dependency제거를 위해 getTimeStamp()메서드 추가
 *   2017.03.03  조성원          시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
 *   2020.10.26  신용호          parseFileInf(List<MultipartFile> files ...) 추가
 *   2022.11.11  김혜준          시큐어코딩 처리
 *   2024.12.04  신용호          downFile() KISA 시큐어코딩 처리
 *   2025.05.26  이백행          PMD로 소프트웨어 보안약점 진단하고 제거하기-FormalParameterNamingConventions(공식 매개변수 명명 규칙), CloseResource(리소스 닫기), LocalVariableNamingConventions(지역 변수 명명 규칙), AssignmentInOperand(피연산자의 할당)
 *   2026.07.15  EricSeokgon     다운로드 Content-Disposition 헤더 이름 수정
 *   2026.08.25  이기하          downFile(request, response) 원본 파일명 속성키를 가이드 기준(orginFile)으로 통일
 *   2026.09.10  실행환경팀        저장 파일명을 실행환경 EgovStoredFileNames(UUID)로, Content-Disposition 헤더를 EgovContentDispositions(RFC 6266)로 이전
 *
 *      </pre>
 */
@Component("EgovFileMngUtil")
public class EgovFileMngUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovFileMngUtil.class);
	private static final String FILE_STORE_PATH = EgovProperties.getProperty("Globals.fileStorePath");

	@Resource(name = "egovFileIdGnrService")
	private EgovIdGnrService idgenService;

	/**
	 * 첨부파일에 대한 목록 정보를 취득한다.
	 *
	 * @param files
	 * @return
	 * @throws Exception
	 */
	public List<FileVO> parseFileInf(Map<String, MultipartFile> files, String keyStr, int fileKeyParam,
			String atchFileId, String storePath) throws Exception {
		int fileKey = fileKeyParam;

		String storePathString = "";
		String atchFileIdString = "";

		if (storePath == null || "".equals(storePath)) {
			storePathString = EgovProperties.getProperty("Globals.fileStorePath");
		} else {
			storePathString = EgovProperties.getProperty(storePath);
		}

		if (atchFileId == null || "".equals(atchFileId)) {
			atchFileIdString = idgenService.getNextStringId();
		} else {
			atchFileIdString = atchFileId;
		}

		File saveFolder = new File(EgovWebUtil.filePathBlackList(storePathString));

		if (!saveFolder.exists() || saveFolder.isFile()) {
			// 2017.03.03 조성원 시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
			if (saveFolder.mkdirs()) {
				LOGGER.debug("[file.mkdirs] saveFolder : Creation Success ");
			} else {
				LOGGER.error("[file.mkdirs] saveFolder : Creation Fail ");
			}
		}

		Iterator<Entry<String, MultipartFile>> itr = files.entrySet().iterator();
		MultipartFile file;
		List<FileVO> result = new ArrayList<FileVO>();
		FileVO fvo;

		while (itr.hasNext()) {
			Entry<String, MultipartFile> entry = itr.next();
			file = entry.getValue();
			String orginFileName = file.getOriginalFilename();
			if (StringUtils.isEmpty(orginFileName)) {
				continue;
			}

			// 2022.11.11 시큐어코딩 처리
			String fileExt = FilenameUtils.getExtension(orginFileName).toUpperCase();
			String newName = storedFileName(keyStr, fileKey);
			long size = file.getSize();
			String filePath = storePathString + File.separator + newName;
			file.transferTo(new File(EgovWebUtil.filePathBlackList(filePath)));

			fvo = new FileVO();
			fvo.setFileExtsn(fileExt);
			fvo.setFileStreCours(storePathString);
			fvo.setFileMg(Long.toString(size));
			fvo.setOrignlFileNm(orginFileName);
			fvo.setStreFileNm(newName);
			fvo.setAtchFileId(atchFileIdString);
			fvo.setFileSn(String.valueOf(fileKey));
			result.add(fvo);

			fileKey++;
		}

		return result;
	}

	/**
	 * 첨부파일에 대한 목록 정보를 취득한다.
	 *
	 * @param files
	 * @return
	 * @throws Exception
	 */
	public List<FileVO> parseFileInf(List<MultipartFile> files, String keyStr, int fileKeyParam, String atchFileId,
			String storePath) throws Exception {
		int fileKey = fileKeyParam;

		String storePathString = "";
		String atchFileIdString = "";

		if (storePath == null || "".equals(storePath)) {
			storePathString = EgovProperties.getProperty("Globals.fileStorePath");
		} else {
			storePathString = EgovProperties.getProperty(storePath);
		}

		if (atchFileId == null || "".equals(atchFileId)) {
			atchFileIdString = idgenService.getNextStringId();
		} else {
			atchFileIdString = atchFileId;
		}

		File saveFolder = new File(EgovWebUtil.filePathBlackList(storePathString));

		if (!saveFolder.exists() || saveFolder.isFile()) {
			// 2017.03.03 조성원 시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
			if (saveFolder.mkdirs()) {
				LOGGER.debug("[file.mkdirs] saveFolder : Creation Success ");
			} else {
				LOGGER.error("[file.mkdirs] saveFolder : Creation Fail ");
			}
		}

		List<FileVO> result = new ArrayList<FileVO>();
		FileVO fvo;

		for (MultipartFile file : files) {

			String orginFileName = file.getOriginalFilename();
			if (StringUtils.isEmpty(orginFileName)) {
				continue;
			}

			// 2022.11.11 시큐어코딩 처리
			String fileExt = FilenameUtils.getExtension(orginFileName).toUpperCase();
			String newName = storedFileName(keyStr, fileKey);
			long size = file.getSize();
			String filePath = storePathString + File.separator + newName;
			file.transferTo(new File(EgovWebUtil.filePathBlackList(filePath)));

			fvo = new FileVO();
			fvo.setFileExtsn(fileExt);
			fvo.setFileStreCours(storePathString);
			fvo.setFileMg(Long.toString(size));
			fvo.setOrignlFileNm(orginFileName);
			fvo.setStreFileNm(newName);
			fvo.setAtchFileId(atchFileIdString);
			fvo.setFileSn(String.valueOf(fileKey));

			result.add(fvo);

			fileKey++;
		}

		return result;
	}

	/**
	 * 첨부파일을 서버에 저장한다.
	 *
	 * @param file
	 * @param newName
	 * @param stordFilePath
	 * @throws Exception
	 */
	protected void writeUploadedFile(MultipartFile file, String newName) throws Exception {
		File cFile = new File(FILE_STORE_PATH);

		if (!cFile.isDirectory()) {
			boolean flag = cFile.mkdir();
			if (!flag) {
				throw new IOException("Directory creation Failed ");
			}
		}

		String writeFilePath = EgovWebUtil
				.filePathBlackList(FILE_STORE_PATH + File.separator + FilenameUtils.getName(newName));

		try (InputStream stream = file.getInputStream(); OutputStream bos = new FileOutputStream(writeFilePath);) {
			FileCopyUtils.copy(stream, bos);
		}
	}

	/**
	 * 서버의 파일을 다운로드한다.
	 *
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public static void downFile(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String downFileName = resolveRequestAttribute(request, "downFile");

		File file = new File(EgovWebUtil.filePathBlackList(FILE_STORE_PATH + downFileName));
		// File file = new File(EgovWebUtil.filePathBlackList(downFileName,FILE_STORE_PATH));

		if (!file.exists()) {
			throw new FileNotFoundException(downFileName);
		}

		if (!file.isFile()) {
			throw new FileNotFoundException(downFileName);
		}

		response.setContentType("application/x-msdownload");
		response.setHeader("Content-Disposition", buildContentDispositionHeader(request));
		response.setHeader("Content-Transfer-Encoding", "binary");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Expires", "0");

		try (BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
				BufferedOutputStream outs = new BufferedOutputStream(response.getOutputStream());) {
			FileCopyUtils.copy(fin, outs);
		}
	}

	/**
	 * request attribute 값을 조회한다. 값이 없으면 빈 문자열을 반환한다.
	 *
	 * @param request
	 * @param attributeName
	 * @return
	 */
	static String resolveRequestAttribute(HttpServletRequest request, String attributeName) {
		Object attributeValue = request.getAttribute(attributeName);
		return (attributeValue == null) ? "" : (String) attributeValue;
	}

	/**
	 * 표준프레임워크 파일 다운로드 가이드가 안내하는 request attribute("orginFile")에서 원본 파일명을 읽어
	 * Content-Disposition 응답 헤더값을 구성한다.
	 *
	 * <p>실행환경 {@link EgovContentDispositions} 에 위임한다 — 한글 파일명은 RFC 5987 {@code filename*=UTF-8''} 로
	 * 인코딩하고 구형 에이전트용 ASCII 폴백을 병기하며, CR/LF 등 제어문자는 걷어내 헤더 인젝션을 차단한다.
	 * 종전 구현은 원본 파일명을 그대로 실어 한글이 깨졌고 CR/LF 만 지웠다.</p>
	 *
	 * @param request
	 * @return
	 */
	static String buildContentDispositionHeader(HttpServletRequest request) {
		return contentDispositionOf(resolveRequestAttribute(request, "orginFile"));
	}

	/**
	 * 원본 파일명으로 attachment 용 Content-Disposition 헤더값을 만든다.
	 *
	 * <p>원본 파일명이 비어 있거나 정리 후 남는 문자가 없으면 파일명 파라미터 없이 {@code attachment} 만 돌려준다
	 * (브라우저가 URL 의 마지막 경로를 파일명으로 쓴다). 종전에는 {@code attachment; filename=} 처럼 값이 빈 헤더가 나갔다.</p>
	 *
	 * @param originalFileName 원본 파일명(신뢰할 수 없는 값 허용)
	 * @return 헤더값
	 */
	static String contentDispositionOf(String originalFileName) {
		if (originalFileName == null || originalFileName.trim().isEmpty()) {
			return "attachment";
		}
		try {
			return EgovContentDispositions.attachment(originalFileName);
		} catch (IllegalArgumentException e) {
			return "attachment";
		}
	}

	/**
	 * 첨부로 등록된 파일을 서버에 업로드한다.
	 *
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static HashMap<String, String> uploadFile(MultipartFile file) throws Exception {

		HashMap<String, String> map = new HashMap<String, String>();
		long size = file.getSize();
		String orginFileName = file.getOriginalFilename();
		String fileExt = "";
		String newName = "";
		// 2022.11.11 시큐어코딩 처리
		if (StringUtils.isNotEmpty(orginFileName)) {
			fileExt = FilenameUtils.getExtension(orginFileName);
		}

		// 2012.11 KISA 보안조치 — 저장명은 원본과 무관한 무작위 값(실행환경 EgovStoredFileNames, UUID 32자)
		newName = EgovStoredFileNames.generateWithoutExtension();
		writeFile(file, newName);
		map.put(Globals.ORIGIN_FILE_NM, orginFileName);
		map.put(Globals.UPLOAD_FILE_NM, newName);
		map.put(Globals.FILE_EXT, fileExt);
		map.put(Globals.FILE_PATH, FILE_STORE_PATH);
		map.put(Globals.FILE_SIZE, String.valueOf(size));

		return map;
	}

	/**
	 * 파일을 실제 물리적인 경로에 생성한다.
	 *
	 * @param file
	 * @param newName
	 * @param stordFilePath
	 * @throws Exception
	 */
	protected static void writeFile(MultipartFile file, String newName) throws Exception {
		File cFile = new File(EgovWebUtil.filePathBlackList(FILE_STORE_PATH));

		if (!cFile.isDirectory()) {
			// 2017.03.03 조성원 시큐어코딩(ES)-부적절한 예외 처리[CWE-253, CWE-440, CWE-754]
			if (cFile.mkdirs()) {
				LOGGER.debug("[file.mkdirs] saveFolder : Creation Success ");
			} else {
				LOGGER.error("[file.mkdirs] saveFolder : Creation Fail ");
			}
		}

		try (InputStream stream = file.getInputStream();
				OutputStream bos = new FileOutputStream(EgovWebUtil
						.filePathBlackList(FILE_STORE_PATH + File.separator + FilenameUtils.getName(newName)));) {

			FileCopyUtils.copy(stream, bos);
		}
	}

	/**
	 * 서버 파일에 대하여 다운로드를 처리한다.
	 *
	 * @param response
	 * @param streFileNm  파일된 파일명
	 * @param orignFileNm
	 * @throws Exception
	 */
	public void downFile(HttpServletResponse response, String streFileNm, String orignFileNm) throws Exception {
		String downFilePath = EgovWebUtil.filePathBlackList(FILE_STORE_PATH + streFileNm);
		// String downFilePath =
		// EgovWebUtil.filePathBlackList(streFileNm,FILE_STORE_PATH);
		String orgFileName = orignFileNm;

		File file = new File(downFilePath);

		if (!file.exists()) {
			throw new FileNotFoundException(downFilePath);
		}

		if (!file.isFile()) {
			throw new FileNotFoundException(downFilePath);
		}

		long fSize = file.length();
		if (fSize > 0) {
			try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));) {
				String mimetype = "application/x-msdownload";

				// response.setBufferSize(fSize);
				response.setContentType(mimetype);
				response.setHeader("Content-Disposition", contentDispositionOf(orgFileName));
				response.setContentLengthLong(fSize);
				// response.setHeader("Content-Transfer-Encoding","binary");
				// response.setHeader("Pragma","no-cache");
				// response.setHeader("Expires","0");
				FileCopyUtils.copy(in, response.getOutputStream());
			}
			response.getOutputStream().flush();
			response.getOutputStream().close();
		}

		/*
		 * String uploadPath = propertiesService.getString("fileDir");
		 * 
		 * File uFile = new File(uploadPath, requestedFile); int fSize = (int)
		 * uFile.length();
		 * 
		 * if (fSize > 0) { BufferedInputStream in = new BufferedInputStream(new
		 * FileInputStream(uFile));
		 * 
		 * String mimetype = "text/html";
		 * 
		 * //response.setBufferSize(fSize); response.setContentType(mimetype);
		 * response.setHeader("Content-Disposition", "attachment; filename=\"" +
		 * requestedFile + "\""); response.setContentLength(fSize);
		 * 
		 * FileCopyUtils.copy(in, response.getOutputStream()); in.close();
		 * response.getOutputStream().flush(); response.getOutputStream().close(); }
		 * else { response.setContentType("text/html"); PrintWriter printwriter =
		 * response.getWriter(); printwriter.println("<html>");
		 * printwriter.println("<br><br><br><h2>Could not get file name:<br>" +
		 * requestedFile + "</h2>"); printwriter.
		 * println("<br><br><br><center><h3><a href='javascript: history.go(-1)'>Back</a></h3></center>"
		 * ); printwriter.println("<br><br><br>&copy; webAccess");
		 * printwriter.println("</html>"); printwriter.flush(); printwriter.close(); }
		 * //
		 */

		/*
		 * response.setContentType("application/x-msdownload");
		 * response.setHeader("Content-Disposition", "attachment; filename=" + new
		 * String(orgFileName.getBytes(),"UTF-8" ));
		 * response.setHeader("Content-Transfer-Encoding","binary");
		 * response.setHeader("Pragma","no-cache"); response.setHeader("Expires","0");
		 * 
		 * BufferedInputStream fin = new BufferedInputStream(new FileInputStream(file));
		 * BufferedOutputStream outs = new
		 * BufferedOutputStream(response.getOutputStream()); int read = 0;
		 * 
		 * while ((read = fin.read(b)) != -1) { outs.write(b,0,read); }
		 * log.debug(this.getClass().getName()
		 * +" BufferedOutputStream Write Complete!!! ");
		 * 
		 * outs.close(); fin.close(); //
		 */
	}

	/**
	 * 첨부파일의 저장 파일명을 만든다 — {@code keyStr + UUID 32자 + fileKey}.
	 *
	 * <p>종전에는 17자리 시각 문자열({@code yyyyMMddhhmmssSSS})을 썼다. 12시간제 {@code hh} 라 오전·오후가 같은
	 * 이름을 만들고, 같은 밀리초에 올라온 파일이 조용히 덮어써졌으며, 시각을 알면 다음 이름을 추측할 수 있었다.
	 * 실행환경 {@link EgovStoredFileNames} 의 UUID 저장명으로 바꿔 세 문제를 없앤다. 확장자는 종전처럼 붙이지 않는다
	 * (FileVO 의 fileExtsn 에 따로 보관). 길이는 접두·순번을 합쳐 40자 안팎으로 STRE_FILE_NM(255) 안에 든다.</p>
	 *
	 * @param keyStr 저장명 접두(예: "FILE_")
	 * @param fileKey 파일 순번
	 * @return 저장 파일명
	 */
	static String storedFileName(String keyStr, int fileKey) {
		return keyStr + EgovStoredFileNames.generateWithoutExtension() + fileKey;
	}
}
