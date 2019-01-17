package uk.ac.ebi.tsc.portal.security;

import org.junit.Test;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.InMemoryResource;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;
import uk.ac.ebi.tsc.portal.api.account.repo.Account;
import uk.ac.ebi.tsc.portal.api.account.repo.AccountRepository;
import uk.ac.ebi.tsc.portal.api.account.service.AccountService;
import uk.ac.ebi.tsc.portal.api.account.service.UserNotFoundException;
import uk.ac.ebi.tsc.portal.api.cloudproviderparameters.repo.CloudProviderParamsCopyRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentConfigurationRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentRepository;
import uk.ac.ebi.tsc.portal.api.deployment.repo.DeploymentStatusRepository;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentConfigurationService;
import uk.ac.ebi.tsc.portal.api.deployment.service.DeploymentService;
import uk.ac.ebi.tsc.portal.api.encryptdecrypt.security.EncryptionService;
import uk.ac.ebi.tsc.portal.api.team.repo.TeamRepository;
import uk.ac.ebi.tsc.portal.clouddeployment.application.ApplicationDeployerBash;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jose A. Dianes <jdianes@ebi.ac.uk>
 */
public class EcpAuthenticationServiceTest {

    private EcpAuthenticationService subject;

    private uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService mockAuthService =
            mock(uk.ac.ebi.tsc.aap.client.security.TokenAuthenticationService.class);

    private AccountService mockAccountService = mock(AccountService.class);
    private TokenService mockTokenService = mock(TokenService.class);
    private DeploymentService mockDeploymentService = mock(DeploymentService.class);
    private DeploymentConfigurationService mockDeploymentConfigurationService = mock(DeploymentConfigurationService.class);
    private DeploymentStatusRepository mockDeploymentStatusRepository = mock(DeploymentStatusRepository.class);
    private CloudProviderParamsCopyRepository mockCloudProviderParamsCopyRepository = mock(CloudProviderParamsCopyRepository.class);
    private TeamRepository mockTeamRepository = mock(TeamRepository.class);
    private ApplicationDeployerBash mockApplicationDeployerBash = mock(ApplicationDeployerBash.class);
    private DomainService mockDomainService = mock(DomainService.class);
    private EncryptionService mockEncryptionService = mock(EncryptionService.class);
    private ResourceLoader mockResourceLoader = mock(ResourceLoader.class);


    public EcpAuthenticationServiceTest() throws IOException {
        when(mockResourceLoader.getResource("ecp.default.teams.file")).thenReturn(new InMemoryResource("[\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.com\",\n" +
                "    \"teamName\": \"TEST1\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.org\",\n" +
                "    \"teamName\": \"TEST2\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"emailDomain\":\"test.org\",\n" +
                "    \"teamName\": \"TEST3\"\n" +
                "  }\n" +
                "]"));
    }

    @Test
    public void
    returns_auth_for_valid_token() {
        User mockUser = mock(User.class);
        when(mockUser.getUsername()).thenReturn("pretend-username");
        when(mockUser.getFullName()).thenReturn("pretend-user-given-name");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("pretend-username");
        when(mockAuth.getDetails()).thenReturn(mockUser);

        HttpServletRequest mockRequest = withAuthorizationHeader("Bearer pretend-valid-token");

        Account mockAccount = mock(Account.class);
        when(mockAccount.getUsername()).thenReturn("pretend-username");
        when(mockAccount.getGivenName()).thenReturn("pretend-user-given-name");
        when(mockAccount.getEmail()).thenReturn("user@domain.com");
        when(mockAccountService.findByUsername("pretend-username")).thenReturn(mockAccount);
        when(mockAccountService.findByUsername("pretend-user-given-name")).thenThrow(UserNotFoundException.class);

        when(mockAuthService.getAuthentication(mockRequest)).thenReturn(mockAuth);
        when(mockTokenService.getAAPToken("ecp-account-username","ecp-account-password")).thenReturn("ecp-aap-pretend-valid-token");

        Authentication auth = subject.getAuthentication(mockRequest);
        assertEquals(auth.getName(), "pretend-username");

    }

    @Test public void
    detects_invalid_jwt() {
        HttpServletRequest request = withAuthorizationHeader("Bearer pretend-invalid-token");
        when(request.getHeader("Authorization").
                equals("Bearer pretend-invalid-token")).thenReturn(null);
        Authentication auth = subject.getAuthentication(request);
        assertNull(auth);
    }

    private HttpServletRequest withAuthorizationHeader(String value) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(value);
        return request;
    }

}
