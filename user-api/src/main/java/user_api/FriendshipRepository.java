package user_api;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUserOneIdAndUserTwoId(Integer userOneId, Integer userTwoId);

    boolean existsByUserOneIdAndUserTwoId(Integer userOneId, Integer userTwoId);

    @Query("""
            SELECT f
            FROM Friendship f
            WHERE f.userOneId = :userId OR f.userTwoId = :userId
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findForUser(@Param("userId") Integer userId);
}
