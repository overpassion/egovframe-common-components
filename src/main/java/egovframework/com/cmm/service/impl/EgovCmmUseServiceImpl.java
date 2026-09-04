package egovframework.com.cmm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.code.EgovCode;
import org.egovframe.rte.fdl.cmmn.code.EgovCodeCache;
import org.springframework.stereotype.Service;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.cmm.service.EgovCmmUseService;
import jakarta.annotation.Resource;

/**
 * 공통코드등 전체 업무에서 공용해서 사용해야 하는 서비스를 정의하기위한 서비스 구현 클래스
 * 
 * @author 공통 서비스 개발팀 이삼섭
 * @since 2009.03.11
 * @version 1.0
 * @see
 *
 *      <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.03.11  이삼섭          최초 생성
 *   2024.10.29  이백행          @Override 표기
 *   2025.07.16  이백행          2025년 컨트리뷰션 `throws Exception` 제거
 *   2026.09.04  실행환경팀        공통코드 조회를 EgovCodeCache 스냅숏으로 이전
 *
 *      </pre>
 */
@Service("EgovCmmUseService")
public class EgovCmmUseServiceImpl extends EgovAbstractServiceImpl implements EgovCmmUseService {

    @Resource(name = "cmmUseDAO")
    private CmmUseDAO cmmUseDAO;

	/** 공통코드 스냅숏 캐시 — 기동 시 전량 적재된다. */
	@Resource(name = "egovCodeCache")
	private EgovCodeCache egovCodeCache;

	/**
	 * 공통코드를 조회한다.
	 *
	 * @param comDefaultCodeVO
	 * @return
	 */
	@Override
	public List<CmmnDetailCode> selectCmmCodeDetail(ComDefaultCodeVO comDefaultCodeVO) {
		return toDetailCodes(comDefaultCodeVO.getCodeId());
	}

	/**
	 * ComDefaultCodeVO의 리스트를 받아서 여러개의 코드 리스트를 맵에 담아서 리턴한다.
	 *
	 * @param comDefaultCodeVOs
	 * @return
	 */
	@Override
	public Map<String, List<CmmnDetailCode>> selectCmmCodeDetails(List<ComDefaultCodeVO> comDefaultCodeVOs) {
		Map<String, List<CmmnDetailCode>> map = new HashMap<>();
		for (ComDefaultCodeVO comDefaultCodeVO : comDefaultCodeVOs) {
			map.put(comDefaultCodeVO.getCodeId(), toDetailCodes(comDefaultCodeVO.getCodeId()));
		}
		return map;
	}

	/**
	 * 캐시에서 사용 중인 코드를 읽어 기존 VO 로 옮긴다.
	 *
	 * <p>반환형을 {@code CmmnDetailCode} 로 유지하는 이유는 호출부 231곳과 화면 131곳이
	 * {@code code}·{@code codeNm} 필드를 직접 참조하기 때문이다. 캐시 도입의 이득
	 * (조회 시 DB 왕복 제거·복수 그룹 조회의 N+1 제거)은 그대로 얻으면서 계약은 바꾸지 않는다.</p>
	 *
	 * @param codeId 코드그룹 ID
	 * @return 사용 중인 코드 목록(없으면 빈 목록)
	 */
	private List<CmmnDetailCode> toDetailCodes(String codeId) {
		List<EgovCode> codes = egovCodeCache.getActiveCodes(codeId);
		List<CmmnDetailCode> result = new ArrayList<>(codes.size());
		for (EgovCode code : codes) {
			CmmnDetailCode detail = new CmmnDetailCode();
			detail.setCodeId(code.getGroupId());
			detail.setCode(code.getCode());
			detail.setCodeNm(code.getName());
			detail.setCodeDc(code.getDescription());
			result.add(detail);
		}
		return result;
	}

	/**
	 * 조직정보를 코드형태로 리턴한다.
	 *
	 * @param 조회조건정보 vo
	 * @return 조직정보 List
	 */
	@Override
	public List<CmmnDetailCode> selectOgrnztIdDetail(ComDefaultCodeVO comDefaultCodeVO) {
		return cmmUseDAO.selectOgrnztIdDetail(comDefaultCodeVO);
	}

	/**
	 * 그룹정보를 코드형태로 리턴한다.
	 *
	 * @param 조회조건정보 vo
	 * @return 그룹정보 List
	 */
	@Override
	public List<CmmnDetailCode> selectGroupIdDetail(ComDefaultCodeVO comDefaultCodeVO) {
		return cmmUseDAO.selectGroupIdDetail(comDefaultCodeVO);
	}
}
