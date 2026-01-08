package ma.xproce.gestion_depenses_projet.service;

import ma.xproce.gestion_depenses_projet.dao.entities.User;

import java.util.Map;
import java.util.Optional;

public interface LastMonthStatisticsService {
    Optional<Map<String, Object>> getLastValidMonth(User user);
}
