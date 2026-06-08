package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.dto.CityRequestDTO;
import br.com.fiap.cityorbit.dto.CityResponseDTO;
import br.com.fiap.cityorbit.exception.ResourceNotFoundException;
import br.com.fiap.cityorbit.service.CityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CityService cityService;

    // ── GET /api/cities ─────────────────────────────────────────────────────

    @Test
    void findAll_deveRetornarListaDeCidades() throws Exception {
        CityResponseDTO city = CityResponseDTO.builder()
                .id(1L).name("São Paulo").state("SP").country("Brasil")
                .latitude(-23.5505).longitude(-46.6333).build();

        when(cityService.findAll()).thenReturn(List.of(city));

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("São Paulo"))
                .andExpect(jsonPath("$[0].state").value("SP"));
    }

    // ── GET /api/cities/{id} ────────────────────────────────────────────────

    @Test
    void findById_deveRetornarCidade() throws Exception {
        CityResponseDTO city = CityResponseDTO.builder()
                .id(1L).name("São Paulo").state("SP").country("Brasil")
                .latitude(-23.5505).longitude(-46.6333).build();

        when(cityService.findById(1L)).thenReturn(city);

        mockMvc.perform(get("/api/cities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("São Paulo"));
    }

    @Test
    void findById_deveRetornar404QuandoNaoEncontrada() throws Exception {
        when(cityService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Cidade", 99L));

        mockMvc.perform(get("/api/cities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── POST /api/cities ────────────────────────────────────────────────────

    @Test
    void create_deveRetornar201QuandoValido() throws Exception {
        CityRequestDTO request = validRequest();

        CityResponseDTO response = CityResponseDTO.builder()
                .id(1L).name("Curitiba").state("PR").country("Brasil")
                .latitude(-25.4290).longitude(-49.2671).build();

        when(cityService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Curitiba"));
    }

    @Test
    void create_deveRetornar400QuandoNomeFaltando() throws Exception {
        CityRequestDTO request = new CityRequestDTO();
        // name ausente — deve falhar na validação
        request.setState("PR");
        request.setCountry("Brasil");
        request.setLatitude(-25.4290);
        request.setLongitude(-49.2671);

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void create_deveRetornar400QuandoLatitudeInvalida() throws Exception {
        CityRequestDTO request = validRequest();
        request.setLatitude(200.0); // inválida

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/cities/{id} ────────────────────────────────────────────────

    @Test
    void update_deveRetornarCidadeAtualizada() throws Exception {
        CityRequestDTO request = validRequest();
        CityResponseDTO response = CityResponseDTO.builder()
                .id(1L).name("Curitiba").state("PR").country("Brasil")
                .latitude(-25.4290).longitude(-49.2671).build();

        when(cityService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/cities/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Curitiba"));
    }

    // ── DELETE /api/cities/{id} ─────────────────────────────────────────────

    @Test
    void delete_deveRetornar204() throws Exception {
        doNothing().when(cityService).delete(1L);

        mockMvc.perform(delete("/api/cities/1"))
                .andExpect(status().isNoContent());

        verify(cityService).delete(1L);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private CityRequestDTO validRequest() {
        CityRequestDTO r = new CityRequestDTO();
        r.setName("Curitiba");
        r.setState("PR");
        r.setCountry("Brasil");
        r.setLatitude(-25.4290);
        r.setLongitude(-49.2671);
        r.setPopulation(1948626L);
        r.setAreaKm2(435.27);
        return r;
    }
}
