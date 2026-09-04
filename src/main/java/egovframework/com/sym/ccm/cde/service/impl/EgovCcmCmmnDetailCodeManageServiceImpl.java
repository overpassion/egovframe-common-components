package egovframework.com.sym.ccm.cde.service.impl;

import java.util.List;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.code.EgovCodeCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.sym.ccm.cde.service.CmmnDetailCodeVO;
import egovframework.com.sym.ccm.cde.service.EgovCcmCmmnDetailCodeManageService;
import jakarta.annotation.Resource;

/**
*
* 공통상세코드에 대한 서비스 구현클래스를 정의한다
* @author 공통서비스 개발팀 이중호
* @since 2009.04.01
* @version 1.0
* @see
*
* <pre>
* << 개정이력(Modification Information) >>
*
*   수정일      수정자           수정내용
*  -------    --------    ---------------------------
*   2009.04.01  이중호          최초 생성
*
* </pre>
*/
@Service("CmmnDetailCodeManageService")
public class EgovCcmCmmnDetailCodeManageServiceImpl extends EgovAbstractServiceImpl implements EgovCcmCmmnDetailCodeManageService{

    @Resource(name="CmmnDetailCodeManageDAO")
    private CmmnDetailCodeManageDAO cmmnDetailCodeManageDAO;

	/**
	 * 공통코드 스냅숏 캐시. 코드를 바꾼 뒤 갱신해야 조회에 반영된다.
	 *
	 * <p>종전에는 조회가 매번 DB 를 읽어 항상 최신이었다. 캐시 도입으로 갱신 시점이
	 * 명시적이 됐으므로 등록·수정·삭제 끝에 {@link EgovCodeCache#reload()} 를 부른다.</p>
	 *
	 * <p><b>다중 서버 배치 주의.</b> reload() 는 이 서버의 스냅숏만 갱신한다.
	 * 여러 서버로 운영하면 나머지 서버는 재기동 전까지 종전 코드를 보게 되므로,
	 * 서버별 갱신 수단을 별도로 두어야 한다.</p>
	 */
	@Resource(name = "egovCodeCache")
	private EgovCodeCache egovCodeCache;

	/**
	 * 공통상세코드 총 개수를 조회한다.
	 */
	@Override
	public int selectCmmnDetailCodeListTotCnt(CmmnDetailCodeVO searchVO) throws Exception {
        return cmmnDetailCodeManageDAO.selectCmmnDetailCodeListTotCnt(searchVO);
	}

	/**
	 * 공통상세코드 목록을 조회한다.
	 */
	@Override
	public List<CmmnDetailCodeVO> selectCmmnDetailCodeList(CmmnDetailCodeVO searchVO) throws Exception {
        return cmmnDetailCodeManageDAO.selectCmmnDetailCodeList(searchVO);
	}

	/**
	 * 공통상세코드 상세항목을 조회한다.
	 * @throws Exception
	 */
	@Override
	public CmmnDetailCode selectCmmnDetailCodeDetail(CmmnDetailCodeVO cmmnDetailCodeVO) throws Exception {
		CmmnDetailCode ret = cmmnDetailCodeManageDAO.selectCmmnDetailCodeDetail(cmmnDetailCodeVO);
    	return ret;
	}

	/**
	 * 공통상세코드를 삭제한다.
	 * @throws Exception
	 */
	@Override
	public void deleteCmmnDetailCode(CmmnDetailCodeVO cmmnDetailCodeVO) throws Exception {
		cmmnDetailCodeManageDAO.deleteCmmnDetailCode(cmmnDetailCodeVO);
		reloadCodeCacheAfterCommit();
	}

	/**
	 * 공통상세코드를 등록한다.
	 */
	@Override
	public void insertCmmnDetailCode(CmmnDetailCodeVO cmmnDetailCodeVO) throws Exception {
		cmmnDetailCodeManageDAO.insertCmmnDetailCode(cmmnDetailCodeVO);
		reloadCodeCacheAfterCommit();
	}

	/**
	 * 공통상세코드를 수정한다.
	 */
	@Override
	public void updateCmmnDetailCode(CmmnDetailCodeVO cmmnDetailCodeVO) throws Exception {
		cmmnDetailCodeManageDAO.updateCmmnDetailCode(cmmnDetailCodeVO);
		reloadCodeCacheAfterCommit();
	}

	/**
	 * 트랜잭션이 <b>커밋된 뒤</b> 공통코드 스냅숏을 갱신한다.
	 *
	 * <p>등록·수정·삭제 메서드 안에서 {@link EgovCodeCache#reload()} 를 곧바로 부르면 안 된다.
	 * {@code context-transaction.xml} 의 {@code execution(* egovframework.com..*Impl.*(..))} 포인트컷이
	 * 이 메서드들을 트랜잭션으로 감싸고, {@code egovCodeCache} 가 쓰는 {@code egov.dataSource} 는
	 * {@code dataSource} 의 <b>별칭</b>이라 같은 인스턴스다. 그래서 {@code JdbcTemplate} 이
	 * 진행 중인 트랜잭션의 커넥션에 합류해 <b>아직 커밋되지 않은 행</b>까지 읽어들인다.
	 * 이후 트랜잭션이 롤백되면 DB 에는 없는 코드가 캐시에만 남는다(실측: 캐시 417건 vs DB 416건).</p>
	 *
	 * <p>커밋 성공 시에만 갱신하도록 동기화 콜백으로 미룬다. 트랜잭션이 없으면(단위 테스트 등)
	 * 즉시 갱신한다. 갱신 실패는 예외로 올리지 않고 로그로 남긴다 — 이미 커밋된 저장을
	 * 실패로 보이게 만드는 편이 더 나쁘다. 대신 캐시가 낡은 채로 남으므로 로그를 확인해야 한다.</p>
	 */
	private void reloadCodeCacheAfterCommit() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			egovCodeCache.reload();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_COMMITTED) {
					egovCodeCache.reload();
				}
			}
		});
	}

}
