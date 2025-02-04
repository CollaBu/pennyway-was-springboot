package kr.co.pennyway.domain.domains.phone.service;

import kr.co.pennyway.common.annotation.DomainService;
import kr.co.pennyway.domain.domains.phone.repository.PhoneCodeRepository;
import kr.co.pennyway.domain.domains.phone.type.PhoneCodeKeyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@DomainService
@RequiredArgsConstructor
public class PhoneCodeRedisService {
    private final PhoneCodeRepository phoneCodeRepository;

    /**
     * 휴대폰 번호와 코드를 저장한다. (5분간 유효)
     * <br>
     * redis에 저장되는 key는 codeType:phone, value는 code이다.
     *
     * @param phone    String : 휴대폰 번호
     * @param code     String : 6자리 정수 코드
     * @param codeType {@link PhoneCodeKeyType} : 코드 타입
     * @return LocalDateTime : 만료 시간
     */
    public LocalDateTime create(String phone, String code, PhoneCodeKeyType codeType) {
        return phoneCodeRepository.save(phone, code, codeType);
    }

    /**
     * 휴대폰 번호로 저장된 코드를 조회한다.
     *
     * @param phone    String : 휴대폰 번호
     * @param codeType {@link PhoneCodeKeyType} : 코드 타입
     * @return String : 6자리 정수 코드
     * @throws IllegalArgumentException : 코드가 없을 경우
     */
    public String readByPhone(String phone, PhoneCodeKeyType codeType) throws IllegalArgumentException {
        try {
            return phoneCodeRepository.findCodeByPhone(phone, codeType);
        } catch (NullPointerException e) {
            log.error("{}:{}에 해당하는 키가 존재하지 않습니다.", phone, codeType);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * 휴대폰 번호로 저장된 데이터의 ttl을 5분으로 연장(롤백)한다.
     *
     * @param phone    String : 휴대폰 번호
     * @param codeType {@link PhoneCodeKeyType} : 코드 타입
     */
    public void extendTimeToLeave(String phone, PhoneCodeKeyType codeType) {
        phoneCodeRepository.extendTimeToLeave(phone, codeType);
    }

    /**
     * 휴대폰 번호로 저장된 코드를 삭제한다.
     *
     * @param phone    String : 휴대폰 번호
     * @param codeType {@link PhoneCodeKeyType} : 코드 타입
     */
    public void delete(String phone, PhoneCodeKeyType codeType) {
        phoneCodeRepository.delete(phone, codeType);
    }
}
