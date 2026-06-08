package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.dto.CityRequestDTO;
import br.com.fiap.cityorbit.dto.CityResponseDTO;
import br.com.fiap.cityorbit.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Tag(name = "Cidades", description = "CRUD de cidades monitoradas via satélite no CityOrbit")
public class CityController {

    private final CityService cityService;

    @GetMapping
    @Operation(summary = "Listar todas as cidades cadastradas")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<List<CityResponseDTO>> findAll() {
        return ResponseEntity.ok(cityService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cidade por ID")
    @ApiResponse(responseCode = "200", description = "Cidade encontrada")
    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    public ResponseEntity<CityResponseDTO> findById(
            @Parameter(description = "ID da cidade") @PathVariable Long id) {
        return ResponseEntity.ok(cityService.findById(id));
    }

    @GetMapping("/country/{country}")
    @Operation(summary = "Buscar cidades por país")
    public ResponseEntity<List<CityResponseDTO>> findByCountry(
            @PathVariable String country) {
        return ResponseEntity.ok(cityService.findByCountry(country));
    }

    @GetMapping("/state/{state}")
    @Operation(summary = "Buscar cidades por estado")
    public ResponseEntity<List<CityResponseDTO>> findByState(
            @PathVariable String state) {
        return ResponseEntity.ok(cityService.findByState(state));
    }

    @PostMapping
    @Operation(summary = "Cadastrar nova cidade",
               description = "Registra uma cidade para monitoramento via satélite/LiDAR")
    @ApiResponse(responseCode = "201", description = "Cidade cadastrada com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou cidade já existente")
    public ResponseEntity<CityResponseDTO> create(
            @Valid @RequestBody CityRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados de uma cidade")
    @ApiResponse(responseCode = "200", description = "Cidade atualizada")
    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    public ResponseEntity<CityResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CityRequestDTO dto) {
        return ResponseEntity.ok(cityService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar cidade")
    @ApiResponse(responseCode = "204", description = "Cidade deletada com sucesso")
    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
