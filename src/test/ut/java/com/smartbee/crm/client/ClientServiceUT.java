package com.smartbee.crm.client;

import com.smartbee.crm.auth.JwtUserDetailsService;
import com.smartbee.crm.client.repo.ClientRepository;
import com.smartbee.crm.client.repo.CrmClient;
import com.smartbee.crm.client.validator.ClientValidator;
import com.smartbee.crm.exception.DataNotFoundException;
import com.smartbee.crm.faker.ClientFaker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PageRequest.class)
public class ClientServiceUT {

    private ClientService clientService;
    private JwtUserDetailsService mockUserDetailsService;
    private ClientRepository mockClientRepository;
    private ClientValidator mockClientValidator;

    public ClientServiceUT() {
        mockUserDetailsService = mock(JwtUserDetailsService.class);
        mockClientRepository = mock(ClientRepository.class);
        mockClientValidator = mock(ClientValidator.class);
        clientService = new ClientService(mockUserDetailsService, mockClientRepository, mockClientValidator);
    }

    @Test
    public void testFindSingleClientByName() {
        final List<CrmClient> clients = Arrays.asList(
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID())
        );
        final Optional<List<CrmClient>> mockClients = Optional.of(clients);
        when(mockClientRepository.findByName(anyString())).thenReturn(mockClients);

        final List<CrmClient> response = clientService.findClientByName("", null, null);

        verify(mockClientRepository).findByName(anyString());
        assertEquals(5, response.size());
    }

    @Test
    public void testPageableFindClientByName() {
        final int page = 0;
        final int size = 5;
        final List<CrmClient> clients = Arrays.asList(
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID()),
                ClientFaker.createClient(UUID.randomUUID())
        );
        final PageImpl<CrmClient> pages = new PageImpl(clients);
        PowerMockito.spy(PageRequest.of(page, size));
        when(mockClientRepository.findByName(anyString(), any())).thenReturn(pages);

        final List<CrmClient> response = clientService.findClientByName("", page, size);

        verify(mockClientRepository).findByName(anyString(), any(PageRequest.class));
        assertEquals(5, response.size());
    }

    @Test
    public void testFindClientById() {
        final CrmClient client = ClientFaker.createClient(UUID.randomUUID());
        when(mockClientRepository.findById(any(UUID.class))).thenReturn(Optional.of(client));

        clientService.findClientById(UUID.randomUUID());

        verify(mockClientRepository).findById(any(UUID.class));
    }

    @Test(expected = DataNotFoundException.class)
    public void testFindNotExistsClientId() {
        when(mockClientRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        clientService.findClientById(UUID.randomUUID());
    }

    @Test
    public void testSaveClient() {
        final UUID userId = UUID.randomUUID();
        final CrmClient client = ClientFaker.createClient(UUID.randomUUID());
        final ArgumentCaptor<CrmClient> clientCaptor = ArgumentCaptor.forClass(CrmClient.class);
        doNothing().when(mockClientValidator).validateCompanyId(any(UUID.class));
        when(mockUserDetailsService.getLoginUser()).thenReturn(userId.toString());
        when(mockClientRepository.save(any(CrmClient.class))).thenReturn(client);

        clientService.saveClient(client);

        verify(mockUserDetailsService).getLoginUser();
        verify(mockClientRepository).save(clientCaptor.capture());
        assertEquals(userId, clientCaptor.getValue().getCreatedBy());
        assertEquals(userId, clientCaptor.getValue().getUpdatedBy());
    }

    @Test
    public void testUpdateClient() {
        final UUID userId = UUID.randomUUID();
        final UUID clientId = UUID.randomUUID();
        final UUID companyId = UUID.randomUUID();
        final CrmClient updatedClient = ClientFaker.createClient(companyId);
        updatedClient.setId(clientId);
        final CrmClient originClient = ClientFaker.createClient(companyId);
        originClient.setId(clientId);
        final ArgumentCaptor<CrmClient> clientCaptor = ArgumentCaptor.forClass(CrmClient.class);
        when(mockUserDetailsService.getLoginUser()).thenReturn(userId.toString());
        when(mockClientRepository.findById(any(UUID.class))).thenReturn(Optional.of(originClient));
        when(mockClientRepository.save(any(CrmClient.class))).thenReturn(updatedClient);

        clientService.updateClient(updatedClient);

        verify(mockUserDetailsService).getLoginUser();
        verify(mockClientRepository).save(clientCaptor.capture());
        assertEquals(userId, clientCaptor.getValue().getUpdatedBy());
    }

    @Test(expected = DataNotFoundException.class)
    public void testUpdateNotExistsClient() {
        final UUID userId = UUID.randomUUID();
        final CrmClient client = ClientFaker.createClient(UUID.randomUUID());
        client.setId(UUID.randomUUID());
        when(mockUserDetailsService.getLoginUser()).thenReturn(userId.toString());
        when(mockClientRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        clientService.updateClient(client);
    }

    @Test
    public void testDeleteClient() {
        final UUID id = UUID.randomUUID();
        doNothing().when(mockClientRepository).deleteById(any(UUID.class));
        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);

        clientService.deleteClient(id);

        verify(mockClientRepository).deleteById(idCaptor.capture());
        assertEquals(id, idCaptor.getValue());
    }

    @Test(expected = DataNotFoundException.class)
    public void testDeleteNotExistsClient() {
        doThrow(new EmptyResultDataAccessException(1)).when(mockClientRepository).deleteById(any(UUID.class));

        clientService.deleteClient(UUID.randomUUID());
    }
}