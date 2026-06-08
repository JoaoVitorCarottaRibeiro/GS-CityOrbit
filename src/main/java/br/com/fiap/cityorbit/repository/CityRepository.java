package br.com.fiap.cityorbit.repository;

import br.com.fiap.cityorbit.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByCountryIgnoreCase(String country);

    List<City> findByStateIgnoreCase(String state);

    boolean existsByNameIgnoreCaseAndStateIgnoreCase(String name, String state);
}
