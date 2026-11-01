package ypfunding.server.check;

import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.function.*;
import ypfunding.server.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CheckMain {
    //파일 경로들
    private static final String SCHEMA_SQL = "schema.sql";
    private static final String SEED_SQL = "check/check-seed.sql";

    //main
    public static void main(String[] args) {
        try(AppContext context = new AppContext()) {
            CheckUsers users = new CheckUsers(context);
            AuthCheck auth = new AuthCheck(context, users);
            MakerCheck maker = new MakerCheck(context, users);
            AdminCheck admin = new AdminCheck(context, users);
            FundingCheck funding = new FundingCheck(context, users);
            try {
                initDB(context);
//                maker.test01ViewFundingStatus(); //프로젝트별 펀딩 현황 보기
//                maker.test02RegisterProject(); //프로젝트 기본 정보 등록
//                maker.test03RegisterReward(); //리워드 등록 및 프로젝트 신청
//                admin.test04ApproveProject(); //프로젝트 등록 승인
//                auth.test05UpdateProfile(); //개인정보 수정
//                admin.test06ViewProjects(); //프로젝트 보기
//                admin.test07FilterProjects(); //프로젝트 필터링
//                funding.test08ApplyReward(); //리워드 신청
//                funding.test09RegisterReview(); //리뷰 및 별점 등록
//                funding.test10ConcurrencyControl(); //제공 가능 수량 관련
            } catch (BusinessException e) {
                System.out.println("업무 규칙에 의해 처리 중단: " + e.getMessage());
            }
        }
    }

    //실행 시 db 초기화 후 세팅
    private static void initDB(AppContext context) {
        List<String> schema = loadStatements(SCHEMA_SQL);
        List<String> seed = loadStatements(SEED_SQL);

        context.getTransactionManager().execute(conn -> {
            try (Statement st = conn.createStatement()) {
                for (String sql : schema) { //테이블 적용
                    st.execute(sql);
                }
                for (String sql : seed) { //seed 적용
                    st.execute(sql);
                }
            }
            return null;
        });
        System.out.println("initDB 동작 완료");
    }

    //여기서만 쓰는 임시 sql리더
    private static List<String> loadStatements(String resource) {
        String text;
        try(InputStream in = CheckMain.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null)
                throw new IllegalStateException("파일없는데?");
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("파일못읽음");
        }

        StringBuilder withoutCommentLines = new StringBuilder();
        for(String line : text.split("\\R")) {
            if(!line.strip().startsWith("--"))
                withoutCommentLines.append(line).append('\n');
        }

        List<String> statements = new ArrayList<>();
        for (String piece : withoutCommentLines.toString().split(";")) {
            String sql = piece.strip();
            if(!sql.isEmpty())
                statements.add(sql);
        }

        return statements;
    }
}
