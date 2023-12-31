    package com.teamtag.tagweb.domain.notice.controller;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.teamtag.tagweb.domain.notice.DTO.*;
    import com.teamtag.tagweb.domain.notice.repository.NoticeRepository;
    import com.teamtag.tagweb.domain.notice.entity.NoticeEntity;
    import com.teamtag.tagweb.domain.notice.service.NoticeService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Paths;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.NoSuchElementException;

    @RestController
    @RequestMapping("/api/notice")
    public class NoticeController {
        private final NoticeService noticeService;
        private final NoticeRepository noticeRepository;

        //noticeService와 noticeRepository의 생성자 주입
        @Autowired
        public NoticeController(NoticeService noticeService, NoticeRepository noticeRepository) {
            this.noticeService = noticeService;
            this.noticeRepository = noticeRepository;
        }


        //첨부파일(이미지)와 [글제목,글내용,추가링크] + 이미지경로를 저장하는 메소드
        @PostMapping(value = "/writeNotice", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        public void writeNotice(@RequestParam("jsonData") String jsonData,
                                @RequestPart(value = "image", required = false) MultipartFile imageData) throws IOException {
            WriteDTO writeDTO = new ObjectMapper().readValue(jsonData, WriteDTO.class);
            if (imageData != null && !imageData.isEmpty()) {
                try {
                    String uploadDir = "src/main/resources/static/img/"; // 이미지를 저장할 디렉토리 경로
                    byte[] bytes = imageData.getBytes();
                    String fileName = imageData.getOriginalFilename();
                    String filePath = uploadDir + fileName;
                    Files.write(Paths.get(filePath), bytes);

                    writeDTO.setImageUrl("img/"+fileName);
                    System.out.println(" 이미지경로 = " + writeDTO);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            noticeService.write(writeDTO);
        }


        //게시물 리스트로 이동하는 페이지 + 페이징 기능 + 검색기능()
        @GetMapping("/list/{page}")
        public ResponseEntity<NoticeListAddPageDTO> NoticeList(@PathVariable String page,
                                                               @RequestParam(required = false) String searchTitle,
                                                               @RequestParam(required = false) String searchContainer) {
            int pageNumber;
            try {
                pageNumber = Integer.parseInt(page);
            } catch (NumberFormatException e) {
                pageNumber = 0;  // 잘못된 페이지 번호가 들어오면 0을 기본값으로 사용
            }
            try {
                Page<NoticeListDTO> noticeList;
                if(searchTitle != null) {
                    noticeList = noticeService.getNoticeListByTitle(pageNumber, searchTitle);
                } else if(searchContainer != null) {
                    noticeList = noticeService.getNoticeListByContent(pageNumber, searchContainer);
                } else {
                    noticeList = noticeService.getNoticeList(pageNumber);
                }
                int totalPage = noticeList.getTotalPages();
                NoticeListAddPageDTO noticeListAddPageDTO = new NoticeListAddPageDTO(noticeList.getContent(), totalPage);
                return new ResponseEntity<>(noticeListAddPageDTO, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        //공지사항 게시물에 직접 들어갔을 때
        @GetMapping("/{id}")
        public NoticeViewDTO updateViewCountAndReturnNoticeDTO(@PathVariable int id) {
            return noticeRepository.findById(id).map(currentNotice -> {
                currentNotice.setViewCount(currentNotice.getViewCount() + 1);
                noticeRepository.save(currentNotice);

                NoticeViewDTO noticeViewDTO = new NoticeViewDTO();
                noticeViewDTO.setTitle(currentNotice.getTitle());
                noticeViewDTO.setWriteTime(currentNotice.getWriteTime());
                noticeViewDTO.setModifyTime(currentNotice.getModifyTime());
                noticeViewDTO.setViewCount(currentNotice.getViewCount());
                noticeViewDTO.setImageUrl(currentNotice.getImageUrl());
                noticeViewDTO.setContents(currentNotice.getContents());
                noticeViewDTO.setLink(currentNotice.getLink());
                System.out.println("이미지 주소ㅋ = " + currentNotice.imageUrl);
                return noticeViewDTO;
            }).orElseThrow(() -> {
                System.out.println("해당 게시물의 ID를 찾을 수 없습니다");
                return new RuntimeException("해당 게시물의 ID를 찾을 수 없습니다");
            });
        }

        //수정하기 버튼 클릭시 기존에 있던 내용을 불러오는 메소드
        @GetMapping("/modify/{id}")
        public ModifyDTO getModifyNotice(@PathVariable int id) {
            NoticeEntity notice = noticeRepository.findById(id)
                    .orElseThrow(() -> {
                        System.out.println("해당 게시물의 ID를 찾을 수 없습니다");
                        return new RuntimeException("해당 게시물의 ID를 찾을 수 없습니다");
                    });
            // DB에서 가져온 데이터를 사용하여 ModifyDTO 객체를 생성
            ModifyDTO modifyDTO = new ModifyDTO();
            modifyDTO.setTitle(notice.getTitle());
            modifyDTO.setContents(notice.getContents());
            modifyDTO.setLink(notice.getLink());
            modifyDTO.setImageUrl(notice.getImageUrl());
            return modifyDTO;
        }


        //수정완료 버튼 클릭시 변경사항을 업데이트하는 메소드.
        @PostMapping(value = "/updateNotice", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        public void modifySave(@RequestParam("jsonData") String jsonData,
                               @RequestPart(value = "image", required = false) MultipartFile imageData) throws IOException {
            ModifyDTO modifyDTO = new ObjectMapper().readValue(jsonData, ModifyDTO.class);
            NoticeEntity notice = noticeRepository.findById(modifyDTO.getId())
                    .orElseThrow(() -> new NoSuchElementException("해당 게시물의 ID를 찾을 수 없습니다."));
            if (imageData != null && !imageData.isEmpty()) {
                String uploadDir = "src/main/resources/static/img/"; // 이미지를 저장할 디렉토리 경로
                byte[] bytes = imageData.getBytes();
                String fileName = imageData.getOriginalFilename();
                String filePath = uploadDir + fileName;

                // 이미지 파일이 새로운 것일 경우, 이전 파일 삭제 및 새로운 파일 저장
                if (!filePath.equals(notice.getImageUrl())) {
                    // 이전 파일 삭제 로직은 여기에 추가합니다.
                    Files.write(Paths.get(filePath), bytes);
                    notice.setImageUrl("img/"+fileName);
                }
            }
            notice.setTitle(modifyDTO.getTitle());
            notice.setContents(modifyDTO.getContents());
            notice.setLink(modifyDTO.getLink());

            LocalDateTime currentTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            notice.setModifyTime(currentTime.format(formatter));
            noticeRepository.save(notice);
        }

        //게시글 삭제하는 메소드
        @PostMapping("/deleteNotice/{id}")
        public void deleteNotice(@PathVariable int id) {
            NoticeEntity notice = noticeRepository.findById(id)
                    .orElseThrow(() -> {
                        System.out.println("해당 게시물의 ID를 찾을 수 없습니다");
                        return new RuntimeException("해당 게시물의 ID를 찾을 수 없습니다");
                    });
            notice.setDeleteNum(1);
            noticeRepository.save(notice);
        }

    }

    //게시판 기능 끝~~~