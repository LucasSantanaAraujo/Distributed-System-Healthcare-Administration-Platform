package com.psoft.clinic.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * ⚙️ Implementação original do serviço de autenticação.
 *
 * ❗ O doctors-service já não faz autenticação diretamente.
 * Este ficheiro pertence ao auth-service e fica aqui apenas
 * para referência até à divisão completa.
 */

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // ⚠️ Este método dependia de UserRepository e User,
        // que foram removidos deste serviço.
        // Deixamos uma implementação neutra só para evitar erros de compilação.
        throw new UsernameNotFoundException("Autenticação desativada neste serviço (doctors-service).");
    }
}
