package egovframework.com.utl.sys.ssy.web;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.egovframe.rte.fdl.filehandling.EgovFiles;
import org.egovframe.rte.fdl.filehandling.upload.EgovUploadPolicy;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.egovframe.rte.ptl.mvc.upload.EgovMultipartFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import egovframework.com.cmm.EgovMessageSource;
import egovframework.com.cmm.LoginVO;
import egovframework.com.cmm.annotation.IncludedInfo;
import egovframework.com.cmm.annotation.RequireAdmin;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.cmm.service.EgovFileMngUtil;
import egovframework.com.cmm.service.EgovProperties;
import egovframework.com.cmm.util.EgovUserDetailsHelper;
import egovframework.com.utl.fcc.service.EgovStringUtil;
import egovframework.com.utl.sys.ssy.service.EgovSynchrnServerService;
import egovframework.com.utl.sys.ssy.service.SynchrnServer;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * <pre>
 * 개요
 * - 동기화대상 서버관리에 대한 controller 클래스를 정의한다.
 *
 * 상세내용
 * - 동기화대상 서버관리에 대한 등록, 수정, 삭제, 조회 기능을 제공한다.
 * - 동기화대상 서버관리의 조회기능은 목록조회, 상세조회로 구분된다.
 * </pre>
 * 
 * @author 이문준
 * @since 2010.06.28
 * @version 1.0
 * @see
 *
 *      <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2010.06.28  이문준          최초 생성
 *   2011.08.26  정진오          IncludedInfo annotation 추가
 *   2019.12.09  신용호          KISA 보안약점 조치 (위험한 형식 파일 업로드)
 *   2025.09.19  이백행          2025년 컨트리뷰션 PMD로 소프트웨어 보안약점 진단하고 제거하기-FieldNamingConventions(변수명에 밑줄 사용)
 *   2025.09.19  이백행          2025년 컨트리뷰션 PMD로 소프트웨어 보안약점 진단하고 제거하기-AvoidReassigningParameters(넘겨받는 메소드 parameter 값을 직접 변경하는 코드 탐지)
 *   2025.09.19  이백행          2025년 컨트리뷰션 PMD로 소프트웨어 보안약점 진단하고 제거하기-SimplifyBooleanExpressions(boolean 사용 시 불필요한 비교 연산을 피하도록 함)
 *   2026.04.20  유지보수        국가사이버안보센터(NCSC)의 보안 점검 결과 반영을 통한 보안패치
 *
 *      </pre>
 */
@Controller
public class EgovSynchrnServerController {

	final static Logger LOGGER = LoggerFactory.getLogger(EgovSynchrnServerController.class);

	@Resource(name = "egovSynchrnServerService")
	private EgovSynchrnServerService egovSynchrnServerService;

	@Resource(name = "egovMessageSource")
	private EgovMessageSource egovMessageSource;

	@Resource(name = "EgovCmmUseService")
	private EgovCmmUseService egovCmmUseService;

	/** ID Generation */
	@Resource(name = "egovSynchrnServerIdGnrService")
	private EgovIdGnrService egovSynchrnServerIdGnrService;

	@Resource(name = "EgovFileMngUtil")
	private EgovFileMngUtil fileUtil;

	final static String SYNTH_SERVER_PATH = EgovProperties.getProperty("Globals.SynchrnServerPath");

	/**
	 * 동기화대상 서버관리 목록화면 이동
	 * 
	 * @return String - 리턴 Url
	 */
	@RequestMapping(value = "/utl/sys/ssy/selectSynchrnServerListView.do")
	public String selectSynchrnServerListView(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model) throws Exception {
		// 파일업로드 제한
		String whiteListFileUploadExtensions = EgovProperties.getProperty("Globals.fileUpload.Extensions");
		String fileUploadMaxSize = EgovProperties.getProperty("Globals.fileUpload.maxSize");

		model.addAttribute("fileUploadExtensions", whiteListFileUploadExtensions);
		model.addAttribute("fileUploadMaxSize", fileUploadMaxSize);

		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(1);
		paginationInfo.setRecordCountPerPage(synchrnServer.getPageUnit());
		paginationInfo.setPageSize(synchrnServer.getPageSize());
		paginationInfo.setTotalRecordCount(0);

		model.addAttribute("paginationInfo", paginationInfo);
		model.addAttribute("synchrnServerList", Collections.emptyList());
		model.addAttribute("fileList", egovSynchrnServerService.getFileName());
		model.addAttribute("message", egovMessageSource.getMessage("success.common.select"));

		return "egovframework/com/utl/sys/ssy/EgovSynchrnServerList";
	}

	/**
	 * 동기화대상 서버정보를 관리하기 위해 등록된 동기화대상 서버목록을 조회한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버(검색·페이징)
	 * @return String - 리턴 Url
	 */
	@IncludedInfo(name = "파일동기화(대상서버)", order = 2150, gid = 90)
	@RequestMapping(value = "/utl/sys/ssy/selectSynchrnServerList.do")
	public String selectSynchrnServerList(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model, HttpServletRequest request) throws Exception {
		// 파일업로드 제한
		String whiteListFileUploadExtensions = EgovProperties.getProperty("Globals.fileUpload.Extensions");
		String fileUploadMaxSize = EgovProperties.getProperty("Globals.fileUpload.maxSize");

		/** paging */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(synchrnServer.getPageIndex());
		paginationInfo.setRecordCountPerPage(synchrnServer.getPageUnit());
		paginationInfo.setPageSize(synchrnServer.getPageSize());

		synchrnServer.setFirstIndex(paginationInfo.getFirstRecordIndex());
		synchrnServer.setLastIndex(paginationInfo.getLastRecordIndex());
		synchrnServer.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		synchrnServer.setSynchrnServerList(egovSynchrnServerService.selectSynchrnServerList(synchrnServer));

		model.addAttribute("synchrnServerList", synchrnServer.getSynchrnServerList());

		int totCnt = egovSynchrnServerService.selectSynchrnServerListTotCnt(synchrnServer);
		paginationInfo.setTotalRecordCount(totCnt);
		model.addAttribute("paginationInfo", paginationInfo);
		model.addAttribute("fileList", egovSynchrnServerService.getFileName());

		Map<String, ?> inputFlash = RequestContextUtils.getInputFlashMap(request);
		if (inputFlash != null && inputFlash.get("message") != null) {
			model.addAttribute("message", inputFlash.get("message"));
		} else {
			model.addAttribute("message", egovMessageSource.getMessage("success.common.select"));
		}

		model.addAttribute("fileUploadExtensions", whiteListFileUploadExtensions);
		model.addAttribute("fileUploadMaxSize", fileUploadMaxSize);

		return "egovframework/com/utl/sys/ssy/EgovSynchrnServerList";
	}

	/**
	 * 등록된 동기화대상 서버의 상세정보를 조회한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버(식별·목록 복귀 조건)
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/getSynchrnServer.do")
	@RequireAdmin
	public String selectSynchrnServer(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model) throws Exception {
		SynchrnServer loaded = egovSynchrnServerService.selectSynchrnServer(synchrnServer);
		if (loaded == null) {
			model.addAttribute("message", egovMessageSource.getMessage("fail.common.select"));
			return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
		}
		model.addAttribute("synchrnServer", loaded);
		model.addAttribute("fileList", egovSynchrnServerService.selectSynchrnServerFiles(loaded));
		model.addAttribute("message", egovMessageSource.getMessage("success.common.select"));

		return "egovframework/com/utl/sys/ssy/EgovSynchrnServerDetail";
	}

	/**
	 * 등록된 동기화대상 서버의 파일을 삭제한다.
	 * 
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/removeSynchrnServerFile.do")
	@RequireAdmin
	public String deleteSynchrnServerFile(@ModelAttribute("synchrnServer") SynchrnServer bound) throws Exception {
		SynchrnServer loaded = egovSynchrnServerService.selectSynchrnServer(bound);
		if (loaded == null) {
			return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
		}
		egovSynchrnServerService.deleteSynchrnServerFile(loaded);
		return "forward:/utl/sys/ssy/getSynchrnServer.do";
	}

	/**
	 * 등록된 동기화대상 서버의 파일을 다운로드 한다.
	 * 
	 * @param serverId        - 동기화대상 서버 ID
	 * @param fileNm          - 다운로드 대상 파일
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/getSynchrnServerFile.do")
	@RequireAdmin
	public String downloadFtpFile(@RequestParam("fileNm") String fileNm, @ModelAttribute("synchrnServer") SynchrnServer bound) throws Exception {
		SynchrnServer loaded = egovSynchrnServerService.selectSynchrnServer(bound);
		if (loaded == null) {
			return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
		}
		loaded.setFilePath(SYNTH_SERVER_PATH);
		egovSynchrnServerService.downloadFtpFile(loaded, fileNm);
		return "forward:/utl/sys/ssy/getSynchrnServer.do";
	}

	/**
	 * 동기화대상 서버정보 등록 화면으로 이동한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버 model
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/addViewSynchrnServer.do")
	@RequireAdmin
	public String insertViewSynchrnServer(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer) throws Exception {
		return "egovframework/com/utl/sys/ssy/EgovSynchrnServerRegist";
	}

	/**
	 * 동기화대상 서버정보를 신규로 등록한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버 model
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/addSynchrnServer.do")
	@RequireAdmin
	public String insertSynchrnServer(@Valid @ModelAttribute("synchrnServer") SynchrnServer synchrnServer, BindingResult bindingResult, RedirectAttributes redirectAttributes) throws Exception {
		if (bindingResult.hasErrors()) {
			return "egovframework/com/utl/sys/ssy/EgovSynchrnServerRegist";
		} else {
			LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
			Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated(); // KISA 보안취약점 조치 (2018-12-10, 이정은)
			if (!isAuthenticated) {
				return "redirect:/uat/uia/egovLoginUsr.do";
			}

			synchrnServer.setFrstRegisterId(user == null ? "" : EgovStringUtil.isNullToString(user.getId()));
			synchrnServer.setLastUpdusrId(user == null ? "" : EgovStringUtil.isNullToString(user.getId()));
			// KISA 보안약점 조치 (2018-10-29, 윤창원)
			if (!EgovStringUtil.isNullToString(synchrnServer.getSynchrnLc()).endsWith("/")) {
				synchrnServer.setSynchrnLc(EgovStringUtil.isNullToString(synchrnServer.getSynchrnLc()).concat("/"));
			}
			synchrnServer.setReflctAt("N");
			synchrnServer.setServerId(egovSynchrnServerIdGnrService.getNextStringId());

			egovSynchrnServerService.insertSynchrnServer(synchrnServer);
			redirectAttributes.addFlashAttribute("message", egovMessageSource.getMessage("success.common.insert"));

			return "redirect:/utl/sys/ssy/selectSynchrnServerList.do";
		}
	}

	/**
	 * 기 등록된 동기화대상 서버정보를 수정하는 화면으로 이동한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버 model
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/updtViewSynchrnServer.do")
	@RequireAdmin
	public String updateViewSynchrnServer(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model) throws Exception {
		model.addAttribute("synchrnServer", egovSynchrnServerService.selectSynchrnServer(synchrnServer));
		model.addAttribute("message", egovMessageSource.getMessage("success.common.select"));
		return "egovframework/com/utl/sys/ssy/EgovSynchrnServerUpdt";
	}

	/**
	 * 기 등록된 동기화대상 서버정보를 수정한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버 model
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/updtSynchrnServer.do")
	@RequireAdmin
	public String updateSynchrnServer(@Valid @ModelAttribute("synchrnServer") SynchrnServer synchrnServer, BindingResult bindingResult, SessionStatus status, Model model) throws Exception {
		if (bindingResult.hasErrors()) {
			return "egovframework/com/utl/sys/ssy/EgovSynchrnServerUpdt";
		} else {
			LoginVO user = (LoginVO) EgovUserDetailsHelper.getAuthenticatedUser();
			Boolean isAuthenticated = EgovUserDetailsHelper.isAuthenticated(); // KISA 보안취약점 조치 (2018-12-10, 이정은)
			if (!isAuthenticated) {
				return "redirect:/uat/uia/egovLoginUsr.do";
			}

			synchrnServer.setLastUpdusrId(user == null ? "" : EgovStringUtil.isNullToString(user.getId()));
			// KISA 보안약점 조치 (2018-10-29, 윤창원)
			if (!EgovStringUtil.isNullToString(synchrnServer.getSynchrnLc()).endsWith("/")) {
				synchrnServer.setSynchrnLc(EgovStringUtil.isNullToString(synchrnServer.getSynchrnLc()).concat("/"));
			}

			egovSynchrnServerService.updateSynchrnServer(synchrnServer);
			status.setComplete();
			model.addAttribute("message", egovMessageSource.getMessage("success.common.update"));

			return "forward:/utl/sys/ssy/getSynchrnServer.do";
		}
	}

	/**
	 * 기 등록된 동기화대상 서버정보를 삭제한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버 model
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/removeSynchrnServer.do")
	@RequireAdmin
	public String deleteSynchrnServer(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, RedirectAttributes redirectAttributes) throws Exception {
		egovSynchrnServerService.deleteSynchrnServer(synchrnServer);
		redirectAttributes.addFlashAttribute("message", egovMessageSource.getMessage("success.common.delete"));
		return "redirect:/utl/sys/ssy/selectSynchrnServerList.do";
	}

	/**
	 * 업로드 파일을 동기화대상 서버들을 대상으로 동기화 처리를 한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/processSynchrn.do")
	@RequireAdmin
	public String processSynchrn(@ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model) throws Exception {
		synchrnServer.setFilePath(SYNTH_SERVER_PATH);
		synchrnServer.setReflctAt("Y");

		File[] syncFiles = egovSynchrnServerService.getSyncLocalFiles();
		if (syncFiles != null) {
			boolean syncResult = egovSynchrnServerService.processSynchrn(synchrnServer, syncFiles);
			if (!syncResult) {
				model.addAttribute("syncResultMessage", egovMessageSource.getMessage("comUtlSysSsy.synchrnServer.nofile.label"));
			}
		}

		return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
	}

	/**
	 * 동기화 대상 파일을 업로드 한다.
	 * 
	 * @param synchrnServer - 동기화대상 서버
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/uploadFile.do")
	@RequireAdmin
	public String uploadFile(final MultipartHttpServletRequest multiRequest, @ModelAttribute("synchrnServer") SynchrnServer synchrnServer, Model model) throws Exception {
		MultipartFile multipartFile = multiRequest.getFile("file");

		if (multipartFile != null) {
			String fileName = multipartFile.getOriginalFilename();

			// 파일업로드 제한 — 확장자 화이트리스트와 최대 크기를 실행환경 정책 하나로 판정한다.
			EgovUploadPolicy policy = uploadPolicy();
			Optional<EgovUploadPolicy.Reason> rejected = EgovMultipartFiles.check(policy, multipartFile);

			if (rejected.isEmpty()) { // 위반 없음 = 허용
				egovSynchrnServerService.writeFile(multipartFile, fileName, synchrnServer);
			} else {
				model.addAttribute("fileUploadResultMessage",
					rejectMessage(rejected.get(), fileName, multipartFile.getSize(), policy));
			}

		}

		return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
	}

	/**
	 * 업로드 정책을 설정에서 만든다.
	 *
	 * <p>종전에는 확장자 검사({@code checkFileExtension})와 크기 검사({@code checkFileMaxSize})가
	 * 따로였다. 실행환경 {@link EgovUploadPolicy} 로 합치면 파일명 없음·빈 파일·확장자 없음까지
	 * 함께 판정된다. 화이트리스트가 비어 있으면 {@code build()} 가 {@link IllegalStateException} 을 던져
	 * 설정 시점에 실패한다(fail-closed) — 종전 {@code isAllowedExtension} 이 빈 목록에서 조용히 {@code false} 를
	 * 돌려주던 것과 달리, 빈 허용 목록이 운영 중에 모든 업로드를 거부하는 상태로 남지 않는다.</p>
	 *
	 * @return 업로드 정책
	 */
	private EgovUploadPolicy uploadPolicy() {
		return EgovUploadPolicy.builder()
			.allowExtensionList(EgovProperties.getProperty("Globals.fileUpload.Extensions"))
			.maxFileSize(Long.parseLong(EgovProperties.getProperty("Globals.fileUpload.maxSize")))
			.build();
	}

	/**
	 * 거부 사유를 화면 메시지로 옮긴다. 확장자·크기 메시지는 종전 문구를 그대로 쓴다.
	 *
	 * @param reason   거부 사유
	 * @param fileName 업로드 파일명
	 * @param fileSize 업로드 파일 크기
	 * @param policy   적용한 정책
	 * @return 화면에 보일 메시지
	 */
	private String rejectMessage(EgovUploadPolicy.Reason reason, String fileName, long fileSize,
			EgovUploadPolicy policy) {
		switch (reason) {
			case SIZE_EXCEEDED:
				return "* 허용되지 않는 파일 사이즈 입니다.[" + fileName + " : " + fileSize + " bytes / "
					+ policy.getMaxFileSize() + " bytes]";
			case NO_FILENAME:
			case EMPTY_FILE:
				// 종전에는 빈 파일이 두 검사를 모두 통과해 writeFile 에서 IOException 으로 터졌다.
				return "* 업로드할 파일을 선택하세요.";
			default:
				return "* 허용되지 않는 확장자 입니다.[" + EgovFiles.getExtension(fileName) + "]";
		}
	}

	/**
	 * 업로드 파일을 삭제한다.
	 * 
	 * @param deleteFiles - 업로드 파일 목록
	 * @return String - 리턴 Url
	 */
	@PostMapping("/utl/sys/ssy/deleteFile.do")
	@RequireAdmin
	public String deleteFile(@RequestParam(value = "deleteFiles", required = false, defaultValue = "") String deleteFiles, @ModelAttribute("synchrnServer") SynchrnServer synchrnServer) throws Exception {
		synchrnServer.setReflctAt("");
		egovSynchrnServerService.deleteFile(deleteFiles, synchrnServer);

		return "forward:/utl/sys/ssy/selectSynchrnServerList.do";
	}

}