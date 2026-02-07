package ru.practicum.analyzer.stats.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.stats.service.AnalyzerService;
import ru.practicum.stats.proto.*;

@Slf4j
@RequiredArgsConstructor
@GrpcService
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerService analyzerService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос на реккомендацию для юзера с ID = {}", request.getUserId());
            analyzerService.getRecommendationsForUser(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка getRecommendationsForUser", e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос на аналогичное событие с ID = {} от юзера с ID = {}", request.getEventId(),
                    request.getUserId());
            analyzerService.getSimilarEvents(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка getSimilarEvents", e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос на подсчет количества взаимодействий для оценки событий с идентификаторами = {}",
                    request.getEventIdList());
            analyzerService.getInteractionsCount(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка getInteractionsCount", e);
            responseObserver.onError(
                    new StatusRuntimeException(Status.INTERNAL.withDescription(e.getLocalizedMessage()).withCause(e)));
        }
    }
}
