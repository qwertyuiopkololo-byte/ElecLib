package com.eleclib.repository;

import com.eleclib.model.User;
import com.eleclib.supabase.SupabaseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private static final String TABLE = "users";
    private static final String PK = "user_id";

    private final SupabaseClient supabase;

    public List<User> findAll() {
        return supabase.getAll(TABLE, User.class);
    }

    public Optional<User> findByLogin(String login) {
        return Optional.ofNullable(supabase.getOne(TABLE, "login", login, User.class));
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(supabase.getById(TABLE, PK, id, User.class));
    }

    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    public User save(User user) {
        if (user.getUserId() == null) {
            // Supabase: только поля таблицы, без user_id (SERIAL)
            Map<String, Object> insert = Map.of(
                    "login", user.getLogin(),
                    "password", user.getPassword(),
                    "first_name", user.getFirstName(),
                    "last_name", user.getLastName(),
                    "role", user.getRole() != null ? user.getRole() : "app_user"
            );
            User created = supabase.post(TABLE, insert, User.class);
            return created != null ? created : user;
        }
        supabase.patch(TABLE, PK, user.getUserId(), user);
        return user;
    }

    public long count() {
        return supabase.getAll(TABLE, User.class).size();
    }
}
