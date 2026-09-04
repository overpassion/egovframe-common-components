package egovframework.com.cmm.web;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the ";License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS"; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import org.egovframe.rte.fdl.filehandling.upload.EgovUploadPolicy;
import org.egovframe.rte.fdl.filehandling.upload.EgovUploadRejectedException;
import org.egovframe.rte.ptl.mvc.upload.EgovMultipartFiles;

import egovframework.com.cmm.service.EgovProperties;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 실행환경의 파일업로드 처리를 위한 기능 클래스
 *
 * @author 공통서비스개발팀 이삼섭
 * @since 2009.06.01
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 *
 *  수정일                수정자             수정내용
 *  ----------   --------    ---------------------------
 *  2009.03.25   이삼섭              최초 생성
 *  2011.06.11   서준식              스프링 3.0 업그레이드 API변경으로인한 수정
 *  2020.10.27   신용호              예외처리 수정
 *  2020.10.29   신용호              허용되지 않는 확장자 업로드 제한 (globals.properties > Globals.fileUpload.Extensions)
 *  2025.07.01   유지보수            Spring Framework 6.2.8, JDK 17 기반으로 업데이트
 *
 *      </pre>
 */
public class EgovMultipartResolver extends StandardServletMultipartResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(EgovMultipartResolver.class);

	public EgovMultipartResolver() {
		super();
	}

	/**
	 * multipart 요청을 파싱하여 파일 업로드 보안 검증을 수행한다.
	 *
	 * @param request HTTP 요청
	 * @param encoding 인코딩
	 * @return MultipartHttpServletRequest
	 * @throws MultipartException multipart 파싱 중 오류 발생 시
	 */
	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		try {
			MultipartHttpServletRequest multipartRequest = super.resolveMultipart(request);

			// 파일 업로드 보안 검증 수행
			validateUploadedFiles(multipartRequest);

			return multipartRequest;
		} catch (MultipartException e) {
			LOGGER.error("Multipart parsing failed: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * 업로드된 파일들의 보안 검증을 수행한다.
	 *
	 * @param multipartRequest MultipartHttpServletRequest
	 * @throws SecurityException 보안 검증 실패 시
	 */
	private void validateUploadedFiles(MultipartHttpServletRequest multipartRequest) throws SecurityException {
		EgovUploadPolicy policy = buildPolicy();

		List<MultipartFile> allFiles = new ArrayList<>();
		for (List<MultipartFile> files : multipartRequest.getMultiFileMap().values()) {
			allFiles.addAll(files);
		}

		try {
			EgovMultipartFiles.validateAll(policy, allFiles);
		} catch (EgovUploadRejectedException e) {
			// 종전 계약 유지 — 이 리졸버는 SecurityException 으로 거부를 알린다.
			LOGGER.warn("File upload rejected: {}", e.getReason());
			throw new SecurityException(e.getMessage(), e);
		}

		LOGGER.debug("File upload validation passed: {} file(s)", allFiles.size());
	}

	/**
	 * 설정에서 업로드 정책을 만든다.
	 *
	 * <p>설정 키와 기본값은 종전과 같다. 달라진 것은 <b>화이트리스트를 지정하지 않으면
	 * 정책을 만들 수 없다</b>는 점이다 — 종전에는 미설정 시 모든 확장자를 통과시켰다.</p>
	 *
	 * @return 업로드 정책
	 * @throws SecurityException 화이트리스트가 설정되지 않았거나 설정값이 잘못된 경우
	 */
	private EgovUploadPolicy buildPolicy() throws SecurityException {
		String extensions = EgovProperties.getProperty("Globals.fileUpload.Extensions");
		if (!StringUtils.hasText(extensions)) {
			throw new SecurityException(
					"Globals.fileUpload.Extensions is not configured. "
					+ "Uploads are rejected until an extension whitelist is set.");
		}

		try {
			return EgovUploadPolicy.builder()
					.allowExtensions(extensions.split(","))
					.maxFileSize(getMaxFileSize())
					.maxFileCount(getMaxFileCount())
					.build();
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw new SecurityException("Invalid file upload policy configuration: " + e.getMessage(), e);
		}
	}

	/**
	 * 최대 파일 개수를 반환한다.
	 *
	 * @return 최대 파일 개수
	 */
	private int getMaxFileCount() {
		String maxFileCountStr = EgovProperties.getProperty("Globals.fileUpload.maxFileCount");
		if (StringUtils.hasText(maxFileCountStr)) {
			try {
				return Integer.parseInt(maxFileCountStr);
			} catch (NumberFormatException e) {
				// 종전에는 warn 로그 후 기본값으로 넘어갔다. 설정 오타가 조용히 무시되면
				// 운영자가 건 제한이 걸리지 않은 채로 뜨므로 즉시 실패한다.
				throw new IllegalStateException(
						"Invalid Globals.fileUpload.maxFileCount: " + maxFileCountStr, e);
			}
		}
		// 기본값: 10개 (Tomcat 9.0.106+ 기본값과 동일)
		return 10;
	}

	/**
	 * 최대 파일 크기를 반환한다.
	 *
	 * @return 최대 파일 크기 (바이트)
	 */
	private long getMaxFileSize() {
		String maxFileSizeStr = EgovProperties.getProperty("Globals.fileUpload.maxSize");
		if (StringUtils.hasText(maxFileSizeStr)) {
			try {
				return Long.parseLong(maxFileSizeStr);
			} catch (NumberFormatException e) {
				// 종전에는 warn 로그 후 기본값으로 넘어갔다(위 maxFileCount 와 같은 이유로 변경).
				throw new IllegalStateException(
						"Invalid Globals.fileUpload.maxSize: " + maxFileSizeStr, e);
			}
		}
		// 기본값: 100MB (수정됨)
		return 100 * 1024 * 1024;
	}

	/**
	 * multipart 요청이 완료된 후 정리 작업을 수행한다.
	 *
	 * @param request HTTP 요청
	 */
	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		// 2026.02.28 KISA 취약점 조치 상위 클래스에서 throwable로 처리
			super.cleanupMultipart(request);
	}

	/**
	 * 요청이 multipart 요청인지 확인한다.
	 *
	 * @param request HTTP 요청
	 * @return multipart 요청 여부
	 */
	@Override
	public boolean isMultipart(HttpServletRequest request) {
		boolean isMultipart = super.isMultipart(request);
		LOGGER.debug("Request isMultipart check result: {}", isMultipart);
		return isMultipart;
	}

}
