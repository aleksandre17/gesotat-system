package org.base.core.repository;

import org.base.core.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    boolean existsByToken(String token);

    @Query("""
        select t from Token t 
        where t.user.id = :userId 
        and t.revoked = false 
        and t.expired = false
        """)
    List<Token> findAllValidTokensByUser(Long userId);

    @Modifying
    @Query("""
        update Token t 
            set t.revoked = true, t.expired = true 
            where t.user.id = :userId 
            and t.tokenType = :tokenType 
            and t.revoked = false
        """)
    void updateAllTokensToRevokedByUserAndType(Long userId, String tokenType);

    @Modifying
    @Query("delete from Token t where t.expirationTime < :now")
    void deleteAllExpiredTokens(Instant now);

}
