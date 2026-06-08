package br.com.fiap.cityorbit.controller;

import br.com.fiap.cityorbit.dto.SimulationRequestDTO;
import br.com.fiap.cityorbit.dto.SimulationResponseDTO;
import br.com.fiap.cityorbit.model.SimulationStatus;
import br.com.fiap.cityorbit.model.SimulationType;
import br.com.fiap.cityorbit.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@Tag(name = "Simulações", description = "Simulações urbanas com dados de satélite e IA — CityOrbit")
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping
    @Operation(summary = "Listar todas as simulações")
    public ResponseEntity<List<SimulationResponseDTO>> findAll() {
        return ResponseEntity.ok(simulationService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar simulação por ID")
    @ApiResponse(responseCode = "404", description = "Simulação não encontrada")
    public ResponseEntity<SimulationResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(simulationService.findById(id));
    }

    @GetMapping("/city/{cityId}")
    @Operation(summary = "Buscar simulações de uma cidade")
    public ResponseEntity<List<SimulationResponseDTO>> findByCityId(@PathVariable Long cityId) {
        return ResponseEntity.ok(simulationService.findByCityId(cityId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Buscar simulações por status (PENDING, PROCESSING, COMPLETED, FAILED)")
    public ResponseEntity<List<SimulationResponseDTO>> findByStatus(
            @PathVariable SimulationStatus status) {
        return ResponseEntity.ok(simulationService.findByStatus(status));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Buscar simulações por tipo (FLOOD, TRAFFIC, CONSTRUCTION, ZONING, HEAT_ISLAND)")
    public ResponseEntity<List<SimulationResponseDTO>> findByType(
            @PathVariable SimulationType type) {
        return ResponseEntity.ok(simulationService.findByType(type));
    }

    @PostMapping
    @Operation(summary = "Criar nova simulação",
               description = "Cria um cenário de simulação para uma cidade. Use fetchNasaData=true para integrar dados climáticos da NASA.")
    @ApiResponse(responseCode = "201", description = "Simulação criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "404", description = "Cidade não encontrada")
    public ResponseEntity<SimulationResponseDTO> create(
            @Valid @RequestBody SimulationRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(simulationService.create(dto));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Processar simulação",
               description = "Executa o processamento da simulação consultando a NASA POWER API para obter dados climáticos reais e calcular o score de risco.")
    @ApiResponse(responseCode = "200", description = "Simulação processada com sucesso")
    @ApiResponse(responseCode = "404", description = "Simulação não encontrada")
    public ResponseEntity<SimulationResponseDTO> process(@PathVariable Long id) {
        return ResponseEntity.ok(simulationService.process(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar simulação")
    public ResponseEntity<SimulationResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody SimulationRequestDTO dto) {
        return ResponseEntity.ok(simulationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar simulação")
    @ApiResponse(responseCode = "204", description = "Simulação deletada")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        simulationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
